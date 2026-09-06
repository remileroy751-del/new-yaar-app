-- Yaar-App / Supabase FINAL SETUP
-- Run this once in the real Supabase SQL Editor (not Logs).
-- Safe to run after Yaar-App-Supabase-Initial-Schema.sql.
-- No Firebase data is migrated.

-- Data API permissions. RLS remains the row-level security boundary.
grant select on public.shops, public.products, public.product_images to anon, authenticated;
grant select, insert, update, delete on public.users to authenticated;
grant select, insert, update, delete on public.shops, public.products, public.product_images to authenticated;
grant select, insert, update, delete on public.interests, public.ad_campaigns to authenticated;
grant select, insert, update, delete on public.conversations, public.messages to authenticated;

-- A shop owner may delete their own interest notifications during account deletion.
drop policy if exists interests_owner_delete on public.interests;
create policy interests_owner_delete
on public.interests
for delete to authenticated
using (shop_owner_id = (select auth.uid()));

-- Secure account deletion. No service_role key is ever embedded in Android.
create or replace function public.delete_my_account()
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if (select auth.uid()) is null then
        raise exception 'not_authenticated';
    end if;

    delete from auth.users
    where id = (select auth.uid());
end;
$$;

revoke all on function public.delete_my_account() from public;
grant execute on function public.delete_my_account() to authenticated;
