create or replace function public.is_service_role_token()
returns boolean
language sql
stable
security invoker
set search_path = public
as $$
    select auth.role() = 'service_role';
$$;

grant execute on function public.is_service_role_token() to anon, authenticated, service_role;
