-- ============================================================
-- YAAR-APP — SUPABASE ALL-IN-ONE SQL
-- VERSION 1.3.0+
-- ============================================================
-- À exécuter dans Supabase SQL Editor.
-- Le script est conçu pour être relançable sans supprimer les données.
-- Aucun secret PayDunya ne doit être placé dans ce fichier.

create extension if not exists pgcrypto;

-- =========================
-- 1. TABLES PRINCIPALES
-- =========================

create table if not exists public.users (
    id uuid primary key references auth.users(id) on delete cascade,
    first_name text not null,
    email text,
    country text not null check (country in (
        'BENIN','BURKINA_FASO','COTE_DIVOIRE','MALI','NIGER','SENEGAL','TOGO'
    )),
    city text not null,
    whatsapp_number text not null unique,
    notifications_enabled boolean not null default true,
    created_at timestamptz not null default now()
);

alter table public.users add column if not exists email text;
create unique index if not exists users_email_lower_unique_idx
    on public.users (lower(email))
    where email is not null and trim(email) <> '';

create table if not exists public.shops (
    id uuid primary key default gen_random_uuid(),
    owner_uid uuid not null references auth.users(id) on delete cascade,
    name text not null,
    whatsapp_number text not null,
    country text not null check (country in (
        'BENIN','BURKINA_FASO','COTE_DIVOIRE','MALI','NIGER','SENEGAL','TOGO'
    )),
    city text not null,
    logo_url text,
    logo_storage_path text,
    activity_description text not null default '',
    categories text[] not null default '{}',
    extra_product_slots integer not null default 0 check (extra_product_slots >= 0),
    certification_status text not null default 'NONE' check (
        certification_status in ('NONE','PENDING','CERTIFIED','REJECTED','EXPIRED')
    ),
    id_card_front_url text,
    id_card_back_url text,
    id_card_front_storage_path text,
    id_card_back_storage_path text,
    certification_requested_at timestamptz,
    certification_paid_at timestamptz,
    certification_expires_at timestamptz,
    created_at timestamptz not null default now()
);

create unique index if not exists shops_one_per_owner_idx on public.shops(owner_uid);

create table if not exists public.products (
    id uuid primary key default gen_random_uuid(),
    shop_id uuid not null references public.shops(id) on delete cascade,
    owner_uid uuid not null references auth.users(id) on delete cascade,
    shop_name text not null,
    name text not null,
    description text not null default '',
    price numeric(14,2) not null default 0 check (price >= 0),
    image_url text not null default '',
    image_storage_path text,
    category text not null default 'Divers',
    country text not null check (country in (
        'BENIN','BURKINA_FASO','COTE_DIVOIRE','MALI','NIGER','SENEGAL','TOGO'
    )),
    city text not null,
    available_cities text[] not null default '{}',
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    activated_at timestamptz not null default now(),
    is_promoted boolean not null default false
);

create index if not exists products_owner_idx on public.products(owner_uid);
create index if not exists products_shop_idx on public.products(shop_id);
create index if not exists products_active_idx on public.products(is_active);
create index if not exists products_country_city_idx on public.products(country, city);

create table if not exists public.product_images (
    id uuid primary key default gen_random_uuid(),
    product_id uuid not null references public.products(id) on delete cascade,
    owner_uid uuid not null references auth.users(id) on delete cascade,
    storage_path text not null,
    public_url text,
    sort_order integer not null default 0,
    created_at timestamptz not null default now()
);
create index if not exists product_images_product_idx on public.product_images(product_id);

create table if not exists public.interests (
    id uuid primary key default gen_random_uuid(),
    product_id uuid not null references public.products(id) on delete cascade,
    product_name text not null,
    product_image_url text not null default '',
    shop_id uuid not null references public.shops(id) on delete cascade,
    shop_owner_id uuid not null references auth.users(id) on delete cascade,
    buyer_id uuid not null references auth.users(id) on delete cascade,
    buyer_first_name text not null,
    buyer_whatsapp_number text not null,
    status text not null default 'PENDING' check (status in ('PENDING','AVAILABLE','UNAVAILABLE')),
    is_read boolean not null default false,
    created_at timestamptz not null default now()
);
create index if not exists interests_shop_owner_idx on public.interests(shop_owner_id, created_at desc);
create index if not exists interests_buyer_idx on public.interests(buyer_id, created_at desc);

create table if not exists public.ad_campaigns (
    id uuid primary key default gen_random_uuid(),
    product_id uuid not null references public.products(id) on delete cascade,
    product_name text not null,
    shop_id uuid not null references public.shops(id) on delete cascade,
    owner_uid uuid not null references auth.users(id) on delete cascade,
    total_expositions integer not null check (total_expositions > 0),
    remaining_expositions integer not null check (remaining_expositions >= 0),
    duration_days integer not null check (duration_days > 0),
    started_at timestamptz not null default now(),
    ends_at timestamptz not null,
    price_fcfa integer not null check (price_fcfa >= 0),
    is_active boolean not null default true
);
create index if not exists ad_campaigns_owner_idx on public.ad_campaigns(owner_uid, is_active);
create index if not exists ad_campaigns_product_idx on public.ad_campaigns(product_id);

-- =========================
-- 2. DISCUSSIONS
-- =========================

create table if not exists public.conversations (
    id text primary key,
    buyer_uid uuid not null references auth.users(id) on delete cascade,
    seller_uid uuid not null references auth.users(id) on delete cascade,
    product_id uuid references public.products(id) on delete set null,
    product_name text not null,
    product_price numeric(14,2) not null default 0,
    shop_name text not null,
    participants uuid[] not null,
    buyer_name text not null default '',
    seller_name text not null default '',
    buyer_whatsapp_number text not null default '',
    seller_whatsapp_number text not null default '',
    last_message text not null default '',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- Compatibilité avec une installation antérieure de la table conversations.
alter table public.conversations add column if not exists buyer_name text not null default '';
alter table public.conversations add column if not exists seller_name text not null default '';
alter table public.conversations add column if not exists buyer_whatsapp_number text not null default '';
alter table public.conversations add column if not exists seller_whatsapp_number text not null default '';
alter table public.conversations add column if not exists last_message text not null default '';

create index if not exists conversations_buyer_idx on public.conversations(buyer_uid, updated_at desc);
create index if not exists conversations_seller_idx on public.conversations(seller_uid, updated_at desc);

create table if not exists public.messages (
    id uuid primary key default gen_random_uuid(),
    conversation_id text not null references public.conversations(id) on delete cascade,
    sender_uid uuid not null references auth.users(id) on delete cascade,
    sender_name text not null,
    text text not null,
    created_at timestamptz not null default now()
);
create index if not exists messages_conversation_idx on public.messages(conversation_id, created_at);

-- =========================
-- 3. PAIEMENTS PAYDUNYA
-- =========================

create table if not exists public.payment_transactions (
    id uuid primary key default gen_random_uuid(),
    user_uid uuid not null references auth.users(id) on delete cascade,
    purpose text not null check (purpose in ('PRODUCT_CAPACITY','PRODUCT_PROMOTION','SHOP_CERTIFICATION')),
    amount_fcfa integer not null check (amount_fcfa > 0),
    description text not null default '',
    product_id uuid references public.products(id) on delete set null,
    shop_id uuid references public.shops(id) on delete set null,
    expositions integer,
    duration_days integer,
    invoice_token text unique,
    checkout_url text,
    status text not null default 'PENDING' check (status in ('PENDING','COMPLETED','CANCELLED','FAILED')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index if not exists payment_transactions_user_idx on public.payment_transactions(user_uid, created_at desc);
create index if not exists payment_transactions_status_idx on public.payment_transactions(status, created_at desc);

-- =========================
-- 4. UPDATED_AT
-- =========================

create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists conversations_set_updated_at on public.conversations;
create trigger conversations_set_updated_at
before update on public.conversations
for each row execute function public.set_updated_at();

drop trigger if exists payment_transactions_set_updated_at on public.payment_transactions;
create trigger payment_transactions_set_updated_at
before update on public.payment_transactions
for each row execute function public.set_updated_at();

-- =========================
-- 5. RLS
-- =========================

alter table public.users enable row level security;
alter table public.shops enable row level security;
alter table public.products enable row level security;
alter table public.product_images enable row level security;
alter table public.interests enable row level security;
alter table public.ad_campaigns enable row level security;
alter table public.conversations enable row level security;
alter table public.messages enable row level security;
alter table public.payment_transactions enable row level security;

drop policy if exists users_select_own on public.users;
create policy users_select_own on public.users for select to authenticated using (id = (select auth.uid()));
drop policy if exists users_insert_own on public.users;
create policy users_insert_own on public.users for insert to authenticated with check (id = (select auth.uid()));
drop policy if exists users_update_own on public.users;
create policy users_update_own on public.users for update to authenticated using (id = (select auth.uid())) with check (id = (select auth.uid()));
drop policy if exists users_delete_own on public.users;
create policy users_delete_own on public.users for delete to authenticated using (id = (select auth.uid()));

drop policy if exists shops_public_select on public.shops;
create policy shops_public_select on public.shops for select to anon, authenticated using (true);
drop policy if exists shops_insert_own on public.shops;
create policy shops_insert_own on public.shops for insert to authenticated with check (owner_uid = (select auth.uid()));
drop policy if exists shops_update_own on public.shops;
create policy shops_update_own on public.shops for update to authenticated using (owner_uid = (select auth.uid())) with check (owner_uid = (select auth.uid()));
drop policy if exists shops_delete_own on public.shops;
create policy shops_delete_own on public.shops for delete to authenticated using (owner_uid = (select auth.uid()));

drop policy if exists products_public_select on public.products;
create policy products_public_select on public.products for select to anon, authenticated using (is_active = true or owner_uid = (select auth.uid()));
drop policy if exists products_insert_own on public.products;
create policy products_insert_own on public.products for insert to authenticated with check (owner_uid = (select auth.uid()) and exists (select 1 from public.shops s where s.id = shop_id and s.owner_uid = (select auth.uid())));
drop policy if exists products_update_own on public.products;
create policy products_update_own on public.products for update to authenticated using (owner_uid = (select auth.uid())) with check (owner_uid = (select auth.uid()) and exists (select 1 from public.shops s where s.id = shop_id and s.owner_uid = (select auth.uid())));
drop policy if exists products_delete_own on public.products;
create policy products_delete_own on public.products for delete to authenticated using (owner_uid = (select auth.uid()));

drop policy if exists product_images_public_select on public.product_images;
create policy product_images_public_select on public.product_images for select to anon, authenticated using (true);
drop policy if exists product_images_insert_own on public.product_images;
create policy product_images_insert_own on public.product_images for insert to authenticated with check (owner_uid = (select auth.uid()));
drop policy if exists product_images_update_own on public.product_images;
create policy product_images_update_own on public.product_images for update to authenticated using (owner_uid = (select auth.uid())) with check (owner_uid = (select auth.uid()));
drop policy if exists product_images_delete_own on public.product_images;
create policy product_images_delete_own on public.product_images for delete to authenticated using (owner_uid = (select auth.uid()));

drop policy if exists interests_participants_select on public.interests;
create policy interests_participants_select on public.interests for select to authenticated using (buyer_id = (select auth.uid()) or shop_owner_id = (select auth.uid()));
drop policy if exists interests_buyer_insert on public.interests;
create policy interests_buyer_insert on public.interests for insert to authenticated with check (buyer_id = (select auth.uid()));
drop policy if exists interests_owner_update on public.interests;
create policy interests_owner_update on public.interests for update to authenticated using (shop_owner_id = (select auth.uid())) with check (shop_owner_id = (select auth.uid()));
drop policy if exists interests_buyer_delete on public.interests;
create policy interests_buyer_delete on public.interests for delete to authenticated using (buyer_id = (select auth.uid()));
drop policy if exists interests_owner_delete on public.interests;
create policy interests_owner_delete on public.interests for delete to authenticated using (shop_owner_id = (select auth.uid()));

drop policy if exists ad_campaigns_owner_select on public.ad_campaigns;
create policy ad_campaigns_owner_select on public.ad_campaigns for select to authenticated using (owner_uid = (select auth.uid()));
drop policy if exists ad_campaigns_owner_insert on public.ad_campaigns;
create policy ad_campaigns_owner_insert on public.ad_campaigns for insert to authenticated with check (owner_uid = (select auth.uid()));
drop policy if exists ad_campaigns_owner_update on public.ad_campaigns;
create policy ad_campaigns_owner_update on public.ad_campaigns for update to authenticated using (owner_uid = (select auth.uid())) with check (owner_uid = (select auth.uid()));
drop policy if exists ad_campaigns_owner_delete on public.ad_campaigns;
create policy ad_campaigns_owner_delete on public.ad_campaigns for delete to authenticated using (owner_uid = (select auth.uid()));

drop policy if exists conversations_participant_select on public.conversations;
create policy conversations_participant_select on public.conversations for select to authenticated using ((select auth.uid()) = any(participants));
drop policy if exists conversations_participant_insert on public.conversations;
create policy conversations_participant_insert on public.conversations for insert to authenticated with check ((select auth.uid()) = any(participants));
drop policy if exists conversations_participant_update on public.conversations;
create policy conversations_participant_update on public.conversations for update to authenticated using ((select auth.uid()) = any(participants)) with check ((select auth.uid()) = any(participants));
drop policy if exists conversations_participant_delete on public.conversations;
create policy conversations_participant_delete on public.conversations for delete to authenticated using ((select auth.uid()) = any(participants));

drop policy if exists messages_participant_select on public.messages;
create policy messages_participant_select on public.messages for select to authenticated using (exists (select 1 from public.conversations c where c.id = conversation_id and (select auth.uid()) = any(c.participants)));
drop policy if exists messages_participant_insert on public.messages;
create policy messages_participant_insert on public.messages for insert to authenticated with check (sender_uid = (select auth.uid()) and exists (select 1 from public.conversations c where c.id = conversation_id and (select auth.uid()) = any(c.participants)));
drop policy if exists messages_sender_delete on public.messages;
create policy messages_sender_delete on public.messages for delete to authenticated using (sender_uid = (select auth.uid()));

-- Le paiement est créé et modifié par les Edge Functions avec service_role.
drop policy if exists payment_transactions_select_own on public.payment_transactions;
create policy payment_transactions_select_own on public.payment_transactions for select to authenticated using (user_uid = (select auth.uid()));

-- =========================
-- 6. STORAGE
-- =========================

insert into storage.buckets (id, name, public)
values ('products','products',true), ('shops','shops',true), ('id_cards','id_cards',false)
on conflict (id) do update set public = excluded.public;

drop policy if exists products_storage_select on storage.objects;
create policy products_storage_select on storage.objects for select to anon, authenticated using (bucket_id = 'products');
drop policy if exists products_storage_insert on storage.objects;
create policy products_storage_insert on storage.objects for insert to authenticated with check (bucket_id = 'products' and (storage.foldername(name))[1] = (select auth.uid())::text);
drop policy if exists products_storage_update on storage.objects;
create policy products_storage_update on storage.objects for update to authenticated using (bucket_id = 'products' and (storage.foldername(name))[1] = (select auth.uid())::text) with check (bucket_id = 'products' and (storage.foldername(name))[1] = (select auth.uid())::text);
drop policy if exists products_storage_delete on storage.objects;
create policy products_storage_delete on storage.objects for delete to authenticated using (bucket_id = 'products' and (storage.foldername(name))[1] = (select auth.uid())::text);

drop policy if exists shops_storage_select on storage.objects;
create policy shops_storage_select on storage.objects for select to anon, authenticated using (bucket_id = 'shops');
drop policy if exists shops_storage_insert on storage.objects;
create policy shops_storage_insert on storage.objects for insert to authenticated with check (bucket_id = 'shops' and (storage.foldername(name))[1] = (select auth.uid())::text);
drop policy if exists shops_storage_update on storage.objects;
create policy shops_storage_update on storage.objects for update to authenticated using (bucket_id = 'shops' and (storage.foldername(name))[1] = (select auth.uid())::text) with check (bucket_id = 'shops' and (storage.foldername(name))[1] = (select auth.uid())::text);
drop policy if exists shops_storage_delete on storage.objects;
create policy shops_storage_delete on storage.objects for delete to authenticated using (bucket_id = 'shops' and (storage.foldername(name))[1] = (select auth.uid())::text);

drop policy if exists id_cards_storage_select on storage.objects;
create policy id_cards_storage_select on storage.objects for select to authenticated using (bucket_id = 'id_cards' and (storage.foldername(name))[1] = (select auth.uid())::text);
drop policy if exists id_cards_storage_insert on storage.objects;
create policy id_cards_storage_insert on storage.objects for insert to authenticated with check (bucket_id = 'id_cards' and (storage.foldername(name))[1] = (select auth.uid())::text);
drop policy if exists id_cards_storage_update on storage.objects;
create policy id_cards_storage_update on storage.objects for update to authenticated using (bucket_id = 'id_cards' and (storage.foldername(name))[1] = (select auth.uid())::text) with check (bucket_id = 'id_cards' and (storage.foldername(name))[1] = (select auth.uid())::text);
drop policy if exists id_cards_storage_delete on storage.objects;
create policy id_cards_storage_delete on storage.objects for delete to authenticated using (bucket_id = 'id_cards' and (storage.foldername(name))[1] = (select auth.uid())::text);

-- =========================
-- 7. DATA API + SUPPRESSION COMPTE
-- =========================

grant select on public.shops, public.products, public.product_images to anon, authenticated;
grant select, insert, update, delete on public.users to authenticated;
grant select, insert, update, delete on public.shops, public.products, public.product_images to authenticated;
grant select, insert, update, delete on public.interests, public.ad_campaigns to authenticated;
grant select, insert, update, delete on public.conversations, public.messages to authenticated;
grant select on public.payment_transactions to authenticated;

create or replace function public.delete_my_account()
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if auth.uid() is null then
        raise exception 'not_authenticated';
    end if;
    delete from auth.users where id = auth.uid();
end;
$$;
revoke all on function public.delete_my_account() from public;
grant execute on function public.delete_my_account() to authenticated;

-- Fin du script.
