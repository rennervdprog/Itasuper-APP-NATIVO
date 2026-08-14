create or replace function public.enqueue_client_status_push()
returns trigger
language plpgsql
security definer
set search_path = public, extensions, vault
as $$
declare
    v_store_name text;
    v_title text;
    v_body text;
    v_supabase_url text;
    v_service_role_key text;
begin
    if new.client_id is null
       or new.status::text not in ('preparando', 'pronto_para_entrega')
       or new.status is not distinct from old.status then
        return new;
    end if;

    select name into v_store_name
    from public.stores
    where id = new.store_id;
    v_store_name := coalesce(v_store_name, 'ItaSuper');

    if new.status::text = 'preparando' then
        v_title := '👨‍🍳 Pedido aceito!';
        v_body := format('Seu pedido #%s no %s está sendo preparado!', upper(left(new.id::text, 8)), v_store_name);
    else
        v_title := '📦 Pedido pronto!';
        v_body := format('Seu pedido #%s no %s está pronto para entrega!', upper(left(new.id::text, 8)), v_store_name);
    end if;

    select decrypted_secret into v_supabase_url
    from vault.decrypted_secrets
    where name = 'supabase_url';
    select decrypted_secret into v_service_role_key
    from vault.decrypted_secrets
    where name = 'service_role_key';

    if v_supabase_url is not null and v_service_role_key is not null then
        perform net.http_post(
            url := v_supabase_url || '/functions/v1/send-push',
            body := jsonb_build_object(
                'user_ids', jsonb_build_array(new.client_id::text),
                'title', v_title,
                'body', v_body,
                'data', jsonb_build_object(
                    'link', '/pedidos',
                    'order_id', new.id::text,
                    'type', 'order_status'
                )
            ),
            headers := jsonb_build_object(
                'Content-Type', 'application/json',
                'Authorization', 'Bearer ' || v_service_role_key,
                'apikey', v_service_role_key
            ),
            timeout_milliseconds := 5000
        );
    else
        raise warning 'Push de status não enfileirado: credenciais ausentes no Vault';
    end if;

    return new;
end;
$$;

revoke all on function public.enqueue_client_status_push() from public;

drop trigger if exists trigger_client_status_push on public.orders;
create trigger trigger_client_status_push
    after update of status on public.orders
    for each row
    execute function public.enqueue_client_status_push();
