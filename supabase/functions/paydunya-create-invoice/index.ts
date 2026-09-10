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

  const body = await req.json()
  const purpose = String(body.purpose || '')
  const amount = Number(body.amountFcfa || 0)
  const description = String(body.description || 'Yaar-App')
  const productId = body.productId ? String(body.productId) : null
  const shopId = body.shopId ? String(body.shopId) : null
  const expositions = body.expositions ? Number(body.expositions) : null
  const durationDays = body.durationDays ? Number(body.durationDays) : null

  if (!['PRODUCT_CAPACITY', 'PRODUCT_PROMOTION', 'SHOP_CERTIFICATION'].includes(purpose)) return json({ error: 'Invalid payment purpose.' }, 400)
  if (!Number.isInteger(amount) || amount <= 0) return json({ error: 'Invalid amount.' }, 400)

  const admin = createClient(supabaseUrl, serviceRoleKey)
  if (!shopId) return json({ error: 'shopId is required.' }, 400)
  const { data: shop, error: shopError } = await admin.from('shops').select('id, owner_uid, name').eq('id', shopId).eq('owner_uid', user.id).single()
  if (shopError || !shop) return json({ error: 'Boutique introuvable ou non autorisée.' }, 403)
  if (purpose === 'PRODUCT_PROMOTION') {
    if (!productId) return json({ error: 'productId is required.' }, 400)
    const { data: product } = await admin.from('products').select('id, shop_id, owner_uid').eq('id', productId).eq('shop_id', shopId).eq('owner_uid', user.id).single()
    if (!product) return json({ error: 'Produit introuvable ou non autorisé.' }, 403)
  }

  const { data: payment, error: insertError } = await admin.from('payment_transactions').insert({
    user_uid: user.id,
    purpose,
    amount_fcfa: amount,
    description,
    product_id: productId,
    shop_id: shopId,
    expositions,
    duration_days: durationDays,
  }).select('id').single()
  if (insertError) return json({ error: insertError.message }, 500)

  const payload = {
    invoice: {
      total_amount: amount,
      description,
      ...(productId ? { items: { item_0: { name: description, quantity: 1, unit_price: amount, total_price: amount } } } : {}),
    },
    store: { name: 'Yaar-App' },
  }

  const response = await fetch('https://app.paydunya.com/api/v1/checkout-invoice/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'PAYDUNYA-MASTER-KEY': masterKey,
      'PAYDUNYA-PRIVATE-KEY': privateKey,
      'PAYDUNYA-TOKEN': token,
    },
    body: JSON.stringify(payload),
  })
  const data = await response.json()
  if (!response.ok || data.response_code !== '00') {
    await admin.from('payment_transactions').update({ status: 'FAILED', updated_at: new Date().toISOString() }).eq('id', payment.id)
    return json({ error: data.response_text || 'PayDunya invoice creation failed.' }, 502)
  }

  await admin.from('payment_transactions').update({
    invoice_token: data.token,
    checkout_url: data.response_text,
    updated_at: new Date().toISOString(),
  }).eq('id', payment.id)

  return json({ paymentId: payment.id, token: data.token, checkoutUrl: data.response_text })
})
