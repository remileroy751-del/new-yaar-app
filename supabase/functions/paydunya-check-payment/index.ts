import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const cors = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
  'Content-Type': 'application/json',
}
const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers: cors })

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: cors })
  if (req.method !== 'POST') return json({ error: 'Method not allowed' }, 405)

  const supabaseUrl = Deno.env.get('SUPABASE_URL')!
  const anonKey = Deno.env.get('SUPABASE_ANON_KEY')!
  const serviceRoleKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
  const masterKey = Deno.env.get('PAYDUNYA_MASTER_KEY')
  const privateKey = Deno.env.get('PAYDUNYA_PRIVATE_KEY')
  const token = Deno.env.get('PAYDUNYA_TOKEN')
  if (!masterKey || !privateKey || !token) return json({ error: 'PayDunya server keys are not configured.' }, 500)

  const authHeader = req.headers.get('Authorization')
  if (!authHeader?.startsWith('Bearer ')) return json({ error: 'Authentication required.' }, 401)
  const accessToken = authHeader.substring(7)
  const authClient = createClient(supabaseUrl, anonKey, { global: { headers: { Authorization: `Bearer ${accessToken}` } } })
  const { data: { user }, error: authError } = await authClient.auth.getUser(accessToken)
  if (authError || !user) return json({ error: 'Invalid authentication.' }, 401)

  const { paymentId } = await req.json()
  if (!paymentId) return json({ error: 'paymentId is required.' }, 400)

  const admin = createClient(supabaseUrl, serviceRoleKey)
  const { data: payment, error: paymentError } = await admin.from('payment_transactions').select('*').eq('id', paymentId).eq('user_uid', user.id).single()
  if (paymentError || !payment) return json({ error: 'Payment not found.' }, 404)
  if (!payment.invoice_token) return json({ status: 'PENDING', message: 'Invoice not created yet.' })

  const response = await fetch(`https://app.paydunya.com/api/v1/checkout-invoice/confirm/${encodeURIComponent(payment.invoice_token)}`, {
    headers: {
      'Content-Type': 'application/json',
      'PAYDUNYA-MASTER-KEY': masterKey,
      'PAYDUNYA-PRIVATE-KEY': privateKey,
      'PAYDUNYA-TOKEN': token,
    },
  })
  const data = await response.json()
  if (!response.ok || data.response_code !== '00') return json({ status: payment.status, message: data.response_text || 'Unable to check payment.' }, 502)

  const rawStatus = String(data.invoice?.status || data.status || '').toUpperCase()
  const status = rawStatus === 'COMPLETED' ? 'COMPLETED' : rawStatus === 'CANCELLED' ? 'CANCELLED' : rawStatus === 'FAILED' ? 'FAILED' : 'PENDING'

  if (status === 'COMPLETED' && payment.status !== 'COMPLETED') {
    await admin.from('payment_transactions').update({ status, updated_at: new Date().toISOString() }).eq('id', paymentId)
  } else if (status !== payment.status) {

    await admin.from('payment_transactions').update({ status, updated_at: new Date().toISOString() }).eq('id', paymentId)
  }

  return json({ status, paymentId })
})
