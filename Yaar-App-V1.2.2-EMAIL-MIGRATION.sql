-- ================================================
-- YAAR-APP V1.2.2 — AUTHENTIFICATION PAR E-MAIL
-- À exécuter UNE FOIS dans Supabase > SQL Editor
-- (pas dans Logs)
-- ================================================

-- Ajoute l'e-mail au profil public Yaar-App sans supprimer les données existantes.
alter table public.users
    add column if not exists email text;

-- Normalisation des éventuelles valeurs existantes.
update public.users
set email = lower(trim(email))
where email is not null;

-- Empêche les doublons d'e-mails réels, sans bloquer d'anciennes lignes de test vides.
create unique index if not exists users_email_unique_idx
    on public.users (lower(email))
    where email is not null and email <> '';

-- Vérification finale.
select id, first_name, email, country, city, whatsapp_number
from public.users
order by created_at desc;
