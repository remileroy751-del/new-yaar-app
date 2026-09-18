-- ============================================================
-- YAAR-APP — V1.5.1 / IMMO + ADMIN + PAIEMENTS
-- À exécuter dans Supabase SQL Editor APRÈS les migrations V1.5
-- Script relançable : ne supprime pas les données existantes.
-- ============================================================

-- ---------- 1. Colonnes nécessaires ----------
alter table public.shops
    add column if not exists extra_immo_slots integer not null default 0;

alter table public.shops
    drop constraint if exists shops_extra_immo_slots_check;
alter table public.shops
    add constraint shops_extra_immo_slots_check check (extra_immo_slots between 0 and 15);

alter table public.products
    add column if not exists listing_type text not null default 'PRODUCT';
alter table public.products
    add column if not exists second_image_url text;
alter table public.shops
    add column if not exists logo_changed_at timestamptz;

alter table public.products
    drop constraint if exists products_listing_type_check;
alter table public.products
    add constraint products_listing_type_check
    check (listing_type in ('PRODUCT','IMMO_SALE','IMMO_RENT'));

create index if not exists products_listing_type_active_idx
    on public.products(listing_type, is_active, created_at desc);

-- ---------- 2. Limite serveur des annonces immobilières ----------
-- Gratuit : 5 annonces actives.
-- Extension : +15, soit 20 annonces actives.
-- Le contrôle est côté base afin qu'un client modifié ne puisse pas contourner la limite.
create or replace function public.enforce_immo_listing_limit()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    allowed_count integer;
    current_count integer;
begin
    if new.is_active = true and new.listing_type in ('IMMO_SALE','IMMO_RENT') then
        select 5 + coalesce(s.extra_immo_slots, 0)
          into allowed_count
          from public.shops s
         where s.id = new.shop_id;

        if allowed_count is null then
            raise exception 'Boutique introuvable.';
        end if;

        select count(*)
          into current_count
          from public.products p
         where p.shop_id = new.shop_id
           and p.is_active = true
           and p.listing_type in ('IMMO_SALE','IMMO_RENT')
           and p.id is distinct from new.id;

        if current_count >= allowed_count then
            raise exception 'IMMO_LIMIT_REACHED: Limite de % annonces immobilières actives atteinte.', allowed_count;
        end if;
    end if;

    return new;
end;
$$;

drop trigger if exists products_immo_limit on public.products;
create trigger products_immo_limit
before insert or update of shop_id, listing_type, is_active
on public.products
for each row execute function public.enforce_immo_listing_limit();

-- ---------- 3. Administration sécurisée ----------
-- IMPORTANT : créez d'abord le compte administrateur dans
-- Supabase > Authentication > Users, puis remplacez l'adresse ci-dessous.
create table if not exists public.yaar_admins (
    email text primary key,
    created_at timestamptz not null default now()
);

alter table public.yaar_admins enable row level security;
drop policy if exists yaar_admins_no_direct_access on public.yaar_admins;
create policy yaar_admins_no_direct_access
on public.yaar_admins
for all to authenticated
using (false)
with check (false);

create or replace function public.is_yaar_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1
          from public.yaar_admins a
         where lower(a.email) = lower(coalesce(auth.jwt() ->> 'email', ''))
    );
$$;

revoke all on function public.is_yaar_admin() from public;
grant execute on function public.is_yaar_admin() to anon, authenticated;

-- >>> REMPLACER CET EMAIL par l'email du compte administrateur Yaar-App.
insert into public.yaar_admins(email)
values ('VOTRE_EMAIL_ADMIN@EXEMPLE.COM')
on conflict (email) do nothing;

-- ---------- 4. Accès administrateur aux statistiques et à la modération ----------
drop policy if exists users_select_admin on public.users;
create policy users_select_admin
on public.users for select to authenticated
using (public.is_yaar_admin());

drop policy if exists products_select_admin on public.products;
create policy products_select_admin
on public.products for select to authenticated
using (public.is_yaar_admin());

drop policy if exists products_delete_admin on public.products;
create policy products_delete_admin
on public.products for delete to authenticated
using (public.is_yaar_admin());

drop policy if exists products_update_admin on public.products;
create policy products_update_admin
on public.products for update to authenticated
using (public.is_yaar_admin())
with check (public.is_yaar_admin());

drop policy if exists shops_update_admin on public.shops;
create policy shops_update_admin
on public.shops for update to authenticated
using (public.is_yaar_admin())
with check (public.is_yaar_admin());

-- ---------- 5. RPC de validation/révocation de boutique ----------
create or replace function public.admin_set_shop_certified(
    p_shop_id uuid,
    p_certified boolean
)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    if not public.is_yaar_admin() then
        raise exception 'not_authorized';
    end if;

    update public.shops
       set certification_status = case when p_certified then 'CERTIFIED' else 'NONE' end,
           certification_paid_at = case when p_certified then coalesce(certification_paid_at, now()) else certification_paid_at end,
           certification_expires_at = case when p_certified then null else certification_expires_at end
     where id = p_shop_id;
end;
$$;

revoke all on function public.admin_set_shop_certified(uuid, boolean) from public;
grant execute on function public.admin_set_shop_certified(uuid, boolean) to authenticated;

-- ---------- 6. Suppression d'un compte par l'administrateur ----------
create or replace function public.admin_delete_user(p_user_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    if not public.is_yaar_admin() then
        raise exception 'not_authorized';
    end if;

    if p_user_id = auth.uid() then
        raise exception 'cannot_delete_self';
    end if;

    delete from auth.users where id = p_user_id;
end;
$$;

revoke all on function public.admin_delete_user(uuid) from public;
grant execute on function public.admin_delete_user(uuid) to authenticated;

-- ---------- 7. Visibilité nationale des annonces immobilières ----------
-- La politique products_public_select existante permet déjà la lecture des produits actifs.
-- L'application Android filtre les annonces IMMO sur le pays de l'utilisateur, sans limiter
-- la visibilité à sa ville : toutes les annonces d'un même pays restent donc visibles partout.

-- ---------- 8. Permissions ----------
grant select on public.users, public.shops, public.products to authenticated;
grant delete, update on public.products, public.shops to authenticated;

-- Fin.
