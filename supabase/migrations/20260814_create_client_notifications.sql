create table if not exists public.client_notifications (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    order_id uuid references public.orders(id) on delete set null,
    notification_type text not null default 'order_update',
    title text not null,
    body text not null default '',
    payload jsonb not null default '{}'::jsonb,
    read_at timestamptz,
    created_at timestamptz not null default now()
);

create index if not exists client_notifications_user_created_idx
    on public.client_notifications (user_id, created_at desc);

create index if not exists client_notifications_user_unread_idx
    on public.client_notifications (user_id, created_at desc)
    where read_at is null;

alter table public.client_notifications enable row level security;

drop policy if exists "Clients can read own notifications" on public.client_notifications;
create policy "Clients can read own notifications"
    on public.client_notifications
    for select
    to authenticated
    using (user_id = (select auth.uid()));

drop policy if exists "Clients can mark own notifications as read" on public.client_notifications;
create policy "Clients can mark own notifications as read"
    on public.client_notifications
    for update
    to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));
