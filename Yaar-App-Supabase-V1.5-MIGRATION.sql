-- Yaar-App V1.5.0 migration: immobilier + logo cooldown
alter table public.products add column if not exists listing_type text not null default 'PRODUCT';
alter table public.products add column if not exists second_image_url text;
alter table public.shops add column if not exists logo_changed_at timestamptz;
alter table public.products drop constraint if exists products_listing_type_check;
alter table public.products add constraint products_listing_type_check check (listing_type in ('PRODUCT','IMMO_SALE','IMMO_RENT'));
create index if not exists products_listing_type_active_idx on public.products(listing_type, is_active, created_at desc);

create or replace function public.enforce_shop_logo_change_cooldown()
returns trigger language plpgsql security definer set search_path=public as $$
begin
  if old.logo_changed_at is not null and new.logo_url is distinct from old.logo_url then
    if now() < old.logo_changed_at + interval '30 days' then
      raise exception 'LOGO_CHANGE_COOLDOWN: Le logo ne peut être modifié qu une fois tous les 30 jours. Prochaine modification: %', to_char(old.logo_changed_at + interval '30 days','DD/MM/YYYY');
    end if;
    new.logo_changed_at := now();
  elsif old.logo_url is distinct from new.logo_url then
    new.logo_changed_at := now();
  else
    new.logo_changed_at := old.logo_changed_at;
  end if;
  return new;
end; $$;

drop trigger if exists shops_logo_change_cooldown on public.shops;
create trigger shops_logo_change_cooldown before update of logo_url on public.shops for each row execute function public.enforce_shop_logo_change_cooldown();

-- Les annonces immobilières n utilisent pas le prix structuré du produit : le prix et les détails sont dans description.
-- Les anciens produits restent PRODUCT par défaut.
