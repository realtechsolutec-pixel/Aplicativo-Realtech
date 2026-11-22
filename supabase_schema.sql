-- ============================================================================
-- RTerp - Schema SQL para Supabase (PostgreSQL + RLS + Storage + Funcionários)
-- Execute este script no SQL Editor do seu projeto Supabase.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Função auxiliar para atualizar updated_at automaticamente
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- 0. FUNCIONÁRIOS (Acesso Operacional sem Acesso Financeiro)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.funcionarios (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  funcionario_user_id UUID UNIQUE REFERENCES auth.users(id) ON DELETE SET NULL,
  nome TEXT NOT NULL,
  email TEXT NOT NULL,
  cargo TEXT NOT NULL DEFAULT 'Técnico Operacional',
  comissao_percentual NUMERIC(5, 2) NOT NULL DEFAULT 50.00 CHECK (comissao_percentual >= 0 AND comissao_percentual <= 100),
  ativo BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (owner_id, email)
);

ALTER TABLE public.funcionarios
  ADD COLUMN IF NOT EXISTS comissao_percentual NUMERIC(5, 2) NOT NULL DEFAULT 50.00;

CREATE INDEX IF NOT EXISTS idx_funcionarios_owner ON public.funcionarios(owner_id);
CREATE INDEX IF NOT EXISTS idx_funcionarios_user ON public.funcionarios(funcionario_user_id);
CREATE INDEX IF NOT EXISTS idx_funcionarios_email ON public.funcionarios(lower(email));
CREATE INDEX IF NOT EXISTS idx_funcionarios_lookup_uid ON public.funcionarios(funcionario_user_id, ativo, owner_id);
CREATE INDEX IF NOT EXISTS idx_funcionarios_lookup_email ON public.funcionarios(lower(email), ativo, owner_id);

DROP TRIGGER IF EXISTS trg_funcionarios_updated_at ON public.funcionarios;
CREATE TRIGGER trg_funcionarios_updated_at
  BEFORE UPDATE ON public.funcionarios
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE OR REPLACE FUNCTION public.check_max_funcionarios_per_owner()
RETURNS TRIGGER AS $$
DECLARE
  current_count INTEGER;
BEGIN
  SELECT COUNT(*) INTO current_count
  FROM public.funcionarios
  WHERE owner_id = NEW.owner_id;

  IF current_count >= 4 THEN
    RAISE EXCEPTION 'Limite máximo de 4 acessos de funcionários por oficina atingido.';
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

DROP TRIGGER IF EXISTS trg_funcionarios_max_4 ON public.funcionarios;
CREATE TRIGGER trg_funcionarios_max_4
  BEFORE INSERT ON public.funcionarios
  FOR EACH ROW EXECUTE FUNCTION public.check_max_funcionarios_per_owner();

ALTER TABLE public.funcionarios ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "funcionarios_select_own_or_self" ON public.funcionarios;
CREATE POLICY "funcionarios_select_own_or_self" ON public.funcionarios
  FOR SELECT USING (
    owner_id = (SELECT public.get_workshop_owner_id())
    OR (SELECT auth.uid()) = owner_id
    OR (SELECT auth.uid()) = funcionario_user_id
    OR lower(email) = lower(COALESCE((SELECT auth.jwt() ->> 'email'), ''))
  );

DROP POLICY IF EXISTS "funcionarios_insert_owner_or_self" ON public.funcionarios;
CREATE POLICY "funcionarios_insert_owner_or_self" ON public.funcionarios
  FOR INSERT WITH CHECK (
    (SELECT auth.uid()) = owner_id
    OR (
      (SELECT auth.uid()) = funcionario_user_id
      AND COALESCE((SELECT auth.jwt() -> 'user_metadata' ->> 'role'), '') = 'FUNCIONARIO'
    )
  );

DROP POLICY IF EXISTS "funcionarios_update_owner_or_claim" ON public.funcionarios;
CREATE POLICY "funcionarios_update_owner_or_claim" ON public.funcionarios
  FOR UPDATE USING (
    (SELECT auth.uid()) = owner_id
    OR (
      funcionario_user_id IS NULL
      AND lower(email) = lower(COALESCE((SELECT auth.jwt() ->> 'email'), ''))
    )
  )
  WITH CHECK (
    (SELECT auth.uid()) = owner_id
    OR (SELECT auth.uid()) = funcionario_user_id
    OR lower(email) = lower(COALESCE((SELECT auth.jwt() ->> 'email'), ''))
  );

DROP POLICY IF EXISTS "funcionarios_delete_owner" ON public.funcionarios;
CREATE POLICY "funcionarios_delete_owner" ON public.funcionarios
  FOR DELETE USING ((SELECT auth.uid()) = owner_id);

-- Funções auxiliares de oficina e permissão (RLS)
CREATE OR REPLACE FUNCTION public.get_workshop_owner_id()
RETURNS UUID
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT COALESCE(
    (
      SELECT owner_id
      FROM public.funcionarios
      WHERE ativo = true
        AND (
          funcionario_user_id = auth.uid()
          OR lower(email) = lower(COALESCE(auth.jwt() ->> 'email', ''))
        )
      LIMIT 1
    ),
    CASE
      WHEN COALESCE(auth.jwt() -> 'user_metadata' ->> 'owner_id', '') ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
      THEN (auth.jwt() -> 'user_metadata' ->> 'owner_id')::uuid
      ELSE NULL
    END,
    auth.uid()
  );
$$;

CREATE OR REPLACE FUNCTION public.is_workshop_admin()
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT NOT EXISTS (
    SELECT 1
    FROM public.funcionarios
    WHERE ativo = true
      AND (
        funcionario_user_id = auth.uid()
        OR lower(email) = lower(COALESCE(auth.jwt() ->> 'email', ''))
      )
  )
  AND COALESCE(auth.jwt() -> 'user_metadata' ->> 'role', 'ADMIN') <> 'FUNCIONARIO';
$$;

-- ============================================================================
-- 1. CONFIGURAÇÕES DA OFICINA / LOJA (Cabeçalho OS, Comprovantes, Garantia)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.configuracoes_oficina (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
  nome_loja TEXT NOT NULL DEFAULT 'RTerp Eletrônica & Segurança',
  cnpj TEXT NOT NULL DEFAULT '',
  telefone TEXT NOT NULL DEFAULT '',
  email TEXT NOT NULL DEFAULT '',
  endereco TEXT NOT NULL DEFAULT '',
  rodape_os TEXT NOT NULL DEFAULT 'Equipamentos não retirados em até 90 dias após aviso estarão sujeitos à taxa de estadia/descarte.',
  garantia_padrao_dias INTEGER NOT NULL DEFAULT 90,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DROP TRIGGER IF EXISTS trg_config_oficina_updated_at ON public.configuracoes_oficina;
CREATE TRIGGER trg_config_oficina_updated_at
  BEFORE UPDATE ON public.configuracoes_oficina
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

ALTER TABLE public.configuracoes_oficina ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "config_select_own" ON public.configuracoes_oficina;
CREATE POLICY "config_select_own" ON public.configuracoes_oficina
  FOR SELECT USING (user_id = (SELECT public.get_workshop_owner_id()));

DROP POLICY IF EXISTS "config_insert_own" ON public.configuracoes_oficina;
CREATE POLICY "config_insert_own" ON public.configuracoes_oficina
  FOR INSERT WITH CHECK ((SELECT auth.uid()) = user_id AND (SELECT public.is_workshop_admin()));

DROP POLICY IF EXISTS "config_update_own" ON public.configuracoes_oficina;
CREATE POLICY "config_update_own" ON public.configuracoes_oficina
  FOR UPDATE USING ((SELECT auth.uid()) = user_id AND (SELECT public.is_workshop_admin()))
  WITH CHECK ((SELECT auth.uid()) = user_id AND (SELECT public.is_workshop_admin()));

-- ============================================================================
-- 2. CLIENTES (CRM - Acesso Operacional e Admin)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.clientes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  nome TEXT NOT NULL,
  tipo TEXT NOT NULL DEFAULT 'PF' CHECK (tipo IN ('PF', 'PJ')),
  documento TEXT DEFAULT '',
  telefone TEXT NOT NULL,
  email TEXT DEFAULT '',
  endereco TEXT DEFAULT '',
  observacoes TEXT DEFAULT '',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_clientes_user_id ON public.clientes(user_id);
CREATE INDEX IF NOT EXISTS idx_clientes_nome ON public.clientes(nome);

DROP TRIGGER IF EXISTS trg_clientes_updated_at ON public.clientes;
CREATE TRIGGER trg_clientes_updated_at
  BEFORE UPDATE ON public.clientes
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

ALTER TABLE public.clientes ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "clientes_select_own" ON public.clientes;
CREATE POLICY "clientes_select_own" ON public.clientes
  FOR SELECT USING (user_id = (SELECT public.get_workshop_owner_id()));

DROP POLICY IF EXISTS "clientes_insert_own" ON public.clientes;
CREATE POLICY "clientes_insert_own" ON public.clientes
  FOR INSERT WITH CHECK (user_id = (SELECT public.get_workshop_owner_id()));

DROP POLICY IF EXISTS "clientes_update_own" ON public.clientes;
CREATE POLICY "clientes_update_own" ON public.clientes
  FOR UPDATE USING (user_id = (SELECT public.get_workshop_owner_id()))
  WITH CHECK (user_id = (SELECT public.get_workshop_owner_id()));

DROP POLICY IF EXISTS "clientes_delete_own" ON public.clientes;
CREATE POLICY "clientes_delete_own" ON public.clientes
  FOR DELETE USING (user_id = (SELECT public.get_workshop_owner_id()));

-- ============================================================================
-- 3. ESTOQUE DE PEÇAS (Acesso Operacional e Admin)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.pecas_estoque (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  codigo_sku TEXT NOT NULL DEFAULT '',
  nome TEXT NOT NULL,
  categoria TEXT NOT NULL DEFAULT 'Componentes Eletrônicos',
  fornecedor TEXT DEFAULT '',
  localizacao TEXT DEFAULT '',
  quantidade INTEGER NOT NULL DEFAULT 0 CHECK (quantidade >= 0),
  estoque_minimo INTEGER NOT NULL DEFAULT 3 CHECK (estoque_minimo >= 0),
  preco_custo NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
  preco_venda NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
  foto_url TEXT NOT NULL DEFAULT '',
  descricao_detalhada TEXT NOT NULL DEFAULT '',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.pecas_estoque
  ADD COLUMN IF NOT EXISTS foto_url TEXT NOT NULL DEFAULT '';
ALTER TABLE public.pecas_estoque
  ADD COLUMN IF NOT EXISTS descricao_detalhada TEXT NOT NULL DEFAULT '';

CREATE INDEX IF NOT EXISTS idx_pecas_user_id ON public.pecas_estoque(user_id);
CREATE INDEX IF NOT EXISTS idx_pecas_estoque_baixo ON public.pecas_estoque(user_id, quantidade, estoque_minimo);

DROP TRIGGER IF EXISTS trg_pecas_updated_at ON public.pecas_estoque;
CREATE TRIGGER trg_pecas_updated_at
  BEFORE UPDATE ON public.pecas_estoque
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

ALTER TABLE public.pecas_estoque ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "pecas_select_own" ON public.pecas_estoque;
CREATE POLICY "pecas_select_own" ON public.pecas_estoque
  FOR SELECT USING (user_id = (SELECT public.get_workshop_owner_id()));

DROP POLICY IF EXISTS "pecas_insert_own" ON public.pecas_estoque;
CREATE POLICY "pecas_insert_own" ON public.pecas_estoque
  FOR INSERT WITH CHECK (user_id = (SELECT public.get_workshop_owner_id()));

DROP POLICY IF EXISTS "pecas_update_own" ON public.pecas_estoque;
CREATE POLICY "pecas_update_own" ON public.pecas_estoque
  FOR UPDATE USING (user_id = (SELECT public.get_workshop_owner_id()))
  WITH CHECK (user_id = (SELECT public.get_workshop_owner_id()));

DROP POLICY IF EXISTS "pecas_delete_own" ON public.pecas_estoque;
CREATE POLICY "pecas_delete_own" ON public.pecas_estoque
  FOR DELETE USING (user_id = (SELECT public.get_workshop_owner_id()));

-- ============================================================================
-- 4. ORDENS DE SERVIÇO (Acesso Operacional e Admin)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.ordens_servico (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  numero_os INTEGER NOT NULL DEFAULT 1001,
  cliente_id UUID REFERENCES public.clientes(id) ON DELETE SET NULL,
  cliente_nome TEXT NOT NULL,
  cliente_telefone TEXT DEFAULT '',
  tecnico_id UUID REFERENCES public.funcionarios(id) ON DELETE SET NULL,
  tecnico_nome TEXT NOT NULL DEFAULT '',
  tecnico_email TEXT NOT NULL DEFAULT '',
  criado_por_nome TEXT NOT NULL DEFAULT '',
  comissao_percentual NUMERIC(5, 2) NOT NULL DEFAULT 0.00 CHECK (comissao_percentual >= 0 AND comissao_percentual <= 100),
  categoria_equipamento TEXT NOT NULL DEFAULT 'Eletrônica Geral',
  equipamento TEXT NOT NULL,
  marca_modelo TEXT DEFAULT '',
  numero_serie TEXT DEFAULT '',
  acessorios_entregues TEXT DEFAULT '',
  defeito_relatado TEXT NOT NULL,
  diagnostico_tecnico TEXT DEFAULT '',
  pecas_utilizadas TEXT DEFAULT '',
  fotos_urls TEXT[] NOT NULL DEFAULT '{}',
  status TEXT NOT NULL DEFAULT 'ABERTA'
    CHECK (status IN ('ABERTA', 'EM_ANALISE', 'AGUARDANDO_PECA', 'APROVADA', 'PRONTA', 'ENTREGUE')),
  prioridade TEXT NOT NULL DEFAULT 'NORMAL'
    CHECK (prioridade IN ('BAIXA', 'NORMAL', 'ALTA', 'URGENTE')),
  valor_mao_obra NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
  valor_pecas NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
  desconto NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
  valor_total NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
  garantia_dias INTEGER NOT NULL DEFAULT 90,
  data_entrada TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  data_saida TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.ordens_servico
  ADD COLUMN IF NOT EXISTS fotos_urls TEXT[] NOT NULL DEFAULT '{}';
ALTER TABLE public.ordens_servico
  ADD COLUMN IF NOT EXISTS tecnico_id UUID REFERENCES public.funcionarios(id) ON DELETE SET NULL;
ALTER TABLE public.ordens_servico
  ADD COLUMN IF NOT EXISTS tecnico_nome TEXT NOT NULL DEFAULT '';
ALTER TABLE public.ordens_servico
  ADD COLUMN IF NOT EXISTS tecnico_email TEXT NOT NULL DEFAULT '';
ALTER TABLE public.ordens_servico
  ADD COLUMN IF NOT EXISTS criado_por_nome TEXT NOT NULL DEFAULT '';
ALTER TABLE public.ordens_servico
  ADD COLUMN IF NOT EXISTS comissao_percentual NUMERIC(5, 2) NOT NULL DEFAULT 0.00;

CREATE INDEX IF NOT EXISTS idx_os_user_id ON public.ordens_servico(user_id);
CREATE INDEX IF NOT EXISTS idx_os_status ON public.ordens_servico(user_id, status);
CREATE INDEX IF NOT EXISTS idx_os_cliente_id ON public.ordens_servico(cliente_id);
CREATE INDEX IF NOT EXISTS idx_os_tecnico_id ON public.ordens_servico(user_id, tecnico_id);

DROP TRIGGER IF EXISTS trg_os_updated_at ON public.ordens_servico;
CREATE TRIGGER trg_os_updated_at
  BEFORE UPDATE ON public.ordens_servico
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

ALTER TABLE public.ordens_servico ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "os_select_own" ON public.ordens_servico;
CREATE POLICY "os_select_own" ON public.ordens_servico
  FOR SELECT USING (user_id = (SELECT public.get_workshop_owner_id()));

DROP POLICY IF EXISTS "os_insert_own" ON public.ordens_servico;
CREATE POLICY "os_insert_own" ON public.ordens_servico
  FOR INSERT WITH CHECK (user_id = (SELECT public.get_workshop_owner_id()));

DROP POLICY IF EXISTS "os_update_own" ON public.ordens_servico;
CREATE POLICY "os_update_own" ON public.ordens_servico
  FOR UPDATE USING (user_id = (SELECT public.get_workshop_owner_id()))
  WITH CHECK (user_id = (SELECT public.get_workshop_owner_id()));

DROP POLICY IF EXISTS "os_delete_own" ON public.ordens_servico;
CREATE POLICY "os_delete_own" ON public.ordens_servico
  FOR DELETE USING (user_id = (SELECT public.get_workshop_owner_id()));

-- ============================================================================
-- 5. FINANCEIRO (BLOQUEADO PARA FUNCIONÁRIOS - SOMENTE ADMIN)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.transacoes_financeiras (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  os_id UUID REFERENCES public.ordens_servico(id) ON DELETE SET NULL,
  descricao TEXT NOT NULL,
  categoria TEXT NOT NULL DEFAULT 'Serviços / Reparo',
  tipo TEXT NOT NULL CHECK (tipo IN ('RECEBER', 'PAGAR')),
  escopo TEXT NOT NULL DEFAULT 'LOJA' CHECK (escopo IN ('LOJA', 'PESSOAL')),
  status TEXT NOT NULL DEFAULT 'PENDENTE' CHECK (status IN ('PENDENTE', 'PAGO')),
  valor NUMERIC(12, 2) NOT NULL CHECK (valor >= 0),
  forma_pagamento TEXT DEFAULT 'PIX',
  data_vencimento DATE NOT NULL DEFAULT CURRENT_DATE,
  data_pagamento DATE,
  is_conta_fixa BOOLEAN NOT NULL DEFAULT false,
  observacoes TEXT DEFAULT '',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.transacoes_financeiras
  ADD COLUMN IF NOT EXISTS is_conta_fixa BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX IF NOT EXISTS idx_fin_user_id ON public.transacoes_financeiras(user_id);
CREATE INDEX IF NOT EXISTS idx_fin_escopo_tipo ON public.transacoes_financeiras(user_id, escopo, tipo, status);

DROP TRIGGER IF EXISTS trg_fin_updated_at ON public.transacoes_financeiras;
CREATE TRIGGER trg_fin_updated_at
  BEFORE UPDATE ON public.transacoes_financeiras
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

ALTER TABLE public.transacoes_financeiras ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "fin_select_own" ON public.transacoes_financeiras;
CREATE POLICY "fin_select_own" ON public.transacoes_financeiras
  FOR SELECT USING ((SELECT auth.uid()) = user_id AND (SELECT public.is_workshop_admin()));

DROP POLICY IF EXISTS "fin_insert_own" ON public.transacoes_financeiras;
CREATE POLICY "fin_insert_own" ON public.transacoes_financeiras
  FOR INSERT WITH CHECK ((SELECT auth.uid()) = user_id AND (SELECT public.is_workshop_admin()));

DROP POLICY IF EXISTS "fin_update_own" ON public.transacoes_financeiras;
CREATE POLICY "fin_update_own" ON public.transacoes_financeiras
  FOR UPDATE USING ((SELECT auth.uid()) = user_id AND (SELECT public.is_workshop_admin()))
  WITH CHECK ((SELECT auth.uid()) = user_id AND (SELECT public.is_workshop_admin()));

DROP POLICY IF EXISTS "fin_delete_own" ON public.transacoes_financeiras;
CREATE POLICY "fin_delete_own" ON public.transacoes_financeiras
  FOR DELETE USING ((SELECT auth.uid()) = user_id AND (SELECT public.is_workshop_admin()));

-- ============================================================================
-- 6. METAS DIÁRIAS DA OFICINA (BLOQUEADO PARA FUNCIONÁRIOS - SOMENTE ADMIN)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.metas_oficina (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
  meta_mensal NUMERIC(12, 2) NOT NULL DEFAULT 15000.00,
  custos_fixos_mensais NUMERIC(12, 2) NOT NULL DEFAULT 3500.00,
  dias_uteis_mes INTEGER NOT NULL DEFAULT 22 CHECK (dias_uteis_mes > 0),
  dias_uteis_restantes INTEGER NOT NULL DEFAULT 15 CHECK (dias_uteis_restantes >= 0),
  ticket_medio_estimado NUMERIC(12, 2) NOT NULL DEFAULT 250.00,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DROP TRIGGER IF EXISTS trg_metas_updated_at ON public.metas_oficina;
CREATE TRIGGER trg_metas_updated_at
  BEFORE UPDATE ON public.metas_oficina
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

ALTER TABLE public.metas_oficina ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "metas_select_own" ON public.metas_oficina;
CREATE POLICY "metas_select_own" ON public.metas_oficina
  FOR SELECT USING ((SELECT auth.uid()) = user_id AND (SELECT public.is_workshop_admin()));

DROP POLICY IF EXISTS "metas_insert_own" ON public.metas_oficina;
CREATE POLICY "metas_insert_own" ON public.metas_oficina
  FOR INSERT WITH CHECK ((SELECT auth.uid()) = user_id AND (SELECT public.is_workshop_admin()));

DROP POLICY IF EXISTS "metas_update_own" ON public.metas_oficina;
CREATE POLICY "metas_update_own" ON public.metas_oficina
  FOR UPDATE USING ((SELECT auth.uid()) = user_id AND (SELECT public.is_workshop_admin()))
  WITH CHECK ((SELECT auth.uid()) = user_id AND (SELECT public.is_workshop_admin()));

-- ============================================================================
-- 7. BUSCA E COMPARATIVO DE PREÇOS POR FORNECEDOR
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.cotacoes_precos (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  produto_nome TEXT NOT NULL,
  codigo_sku TEXT DEFAULT '',
  fornecedor TEXT NOT NULL,
  preco_unitario NUMERIC(12, 2) NOT NULL CHECK (preco_unitario >= 0),
  frete NUMERIC(12, 2) NOT NULL DEFAULT 0.00 CHECK (frete >= 0),
  prazo_entrega_dias INTEGER NOT NULL DEFAULT 2,
  observacoes TEXT DEFAULT '',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cotacoes_user_id ON public.cotacoes_precos(user_id);
CREATE INDEX IF NOT EXISTS idx_cotacoes_produto ON public.cotacoes_precos(user_id, produto_nome);

DROP TRIGGER IF EXISTS trg_cotacoes_updated_at ON public.cotacoes_precos;
CREATE TRIGGER trg_cotacoes_updated_at
  BEFORE UPDATE ON public.cotacoes_precos
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

ALTER TABLE public.cotacoes_precos ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "cotacoes_select_own" ON public.cotacoes_precos;
CREATE POLICY "cotacoes_select_own" ON public.cotacoes_precos
  FOR SELECT USING (user_id = (SELECT public.get_workshop_owner_id()));

DROP POLICY IF EXISTS "cotacoes_insert_own" ON public.cotacoes_precos;
CREATE POLICY "cotacoes_insert_own" ON public.cotacoes_precos
  FOR INSERT WITH CHECK (user_id = (SELECT public.get_workshop_owner_id()));

DROP POLICY IF EXISTS "cotacoes_update_own" ON public.cotacoes_precos;
CREATE POLICY "cotacoes_update_own" ON public.cotacoes_precos
  FOR UPDATE USING (user_id = (SELECT public.get_workshop_owner_id()))
  WITH CHECK (user_id = (SELECT public.get_workshop_owner_id()));

DROP POLICY IF EXISTS "cotacoes_delete_own" ON public.cotacoes_precos;
CREATE POLICY "cotacoes_delete_own" ON public.cotacoes_precos
  FOR DELETE USING (user_id = (SELECT public.get_workshop_owner_id()));

-- ============================================================================
-- 8. PEDIDOS DE PEÇAS FALTANTES / PEÇAS PARA CONCLUIR OS (Operacional e Admin)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.pedidos_pecas (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  peca_id UUID REFERENCES public.pecas_estoque(id) ON DELETE SET NULL,
  os_id UUID REFERENCES public.ordens_servico(id) ON DELETE SET NULL,
  numero_os INTEGER,
  nome_peca TEXT NOT NULL,
  codigo_sku TEXT NOT NULL DEFAULT '',
  quantidade INTEGER NOT NULL DEFAULT 1 CHECK (quantidade > 0),
  motivo TEXT NOT NULL DEFAULT 'PARA_OS' CHECK (motivo IN ('FALTANTE', 'PARA_OS')),
  status TEXT NOT NULL DEFAULT 'PENDENTE' CHECK (status IN ('PENDENTE', 'PEDIDO_FEITO', 'RECEBIDA')),
  prioridade TEXT NOT NULL DEFAULT 'NORMAL' CHECK (prioridade IN ('BAIXA', 'NORMAL', 'ALTA', 'URGENTE')),
  solicitado_por TEXT NOT NULL DEFAULT '',
  fornecedor_sugerido TEXT NOT NULL DEFAULT '',
  observacoes TEXT NOT NULL DEFAULT '',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pedidos_pecas_user_id ON public.pedidos_pecas(user_id);
CREATE INDEX IF NOT EXISTS idx_pedidos_pecas_status ON public.pedidos_pecas(user_id, status);

DROP TRIGGER IF EXISTS trg_pedidos_pecas_updated_at ON public.pedidos_pecas;
CREATE TRIGGER trg_pedidos_pecas_updated_at
  BEFORE UPDATE ON public.pedidos_pecas
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

ALTER TABLE public.pedidos_pecas ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "pedidos_pecas_select_own" ON public.pedidos_pecas;
CREATE POLICY "pedidos_pecas_select_own" ON public.pedidos_pecas
  FOR SELECT USING (user_id = (SELECT public.get_workshop_owner_id()));

DROP POLICY IF EXISTS "pedidos_pecas_insert_own" ON public.pedidos_pecas;
CREATE POLICY "pedidos_pecas_insert_own" ON public.pedidos_pecas
  FOR INSERT WITH CHECK (user_id = (SELECT public.get_workshop_owner_id()));

DROP POLICY IF EXISTS "pedidos_pecas_update_own" ON public.pedidos_pecas;
CREATE POLICY "pedidos_pecas_update_own" ON public.pedidos_pecas
  FOR UPDATE USING (user_id = (SELECT public.get_workshop_owner_id()))
  WITH CHECK (user_id = (SELECT public.get_workshop_owner_id()));

DROP POLICY IF EXISTS "pedidos_pecas_delete_own" ON public.pedidos_pecas;
CREATE POLICY "pedidos_pecas_delete_own" ON public.pedidos_pecas
  FOR DELETE USING (user_id = (SELECT public.get_workshop_owner_id()));

-- ============================================================================
-- 8B. VENDAS / PDV INTEGRADO AO ESTOQUE E FINANCEIRO (Escala 200+ Contas ADM)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.vendas (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  numero_venda INTEGER NOT NULL DEFAULT 1,
  cliente_id UUID REFERENCES public.clientes(id) ON DELETE SET NULL,
  cliente_nome TEXT NOT NULL DEFAULT 'Consumidor Final',
  cliente_telefone TEXT NOT NULL DEFAULT '',
  vendedor_id UUID REFERENCES public.funcionarios(id) ON DELETE SET NULL,
  vendedor_nome TEXT NOT NULL DEFAULT '',
  forma_pagamento TEXT NOT NULL DEFAULT 'PIX',
  status TEXT NOT NULL DEFAULT 'CONCLUIDA' CHECK (status IN ('CONCLUIDA', 'PENDENTE', 'CANCELADA')),
  subtotal NUMERIC(12, 2) NOT NULL DEFAULT 0.00 CHECK (subtotal >= 0),
  desconto NUMERIC(12, 2) NOT NULL DEFAULT 0.00 CHECK (desconto >= 0),
  valor_total NUMERIC(12, 2) NOT NULL DEFAULT 0.00 CHECK (valor_total >= 0),
  observacoes TEXT NOT NULL DEFAULT '',
  itens JSONB NOT NULL DEFAULT '[]'::jsonb,
  data_venda TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_vendas_user_data ON public.vendas(user_id, data_venda DESC);
CREATE INDEX IF NOT EXISTS idx_vendas_user_num ON public.vendas(user_id, numero_venda DESC);
CREATE INDEX IF NOT EXISTS idx_vendas_user_status ON public.vendas(user_id, status);

DROP TRIGGER IF EXISTS trg_vendas_updated_at ON public.vendas;
CREATE TRIGGER trg_vendas_updated_at
  BEFORE UPDATE ON public.vendas
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

ALTER TABLE public.vendas ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "vendas_select_own" ON public.vendas;
CREATE POLICY "vendas_select_own" ON public.vendas
  FOR SELECT USING (user_id = (SELECT public.get_workshop_owner_id()));

DROP POLICY IF EXISTS "vendas_insert_own" ON public.vendas;
CREATE POLICY "vendas_insert_own" ON public.vendas
  FOR INSERT WITH CHECK (user_id = (SELECT public.get_workshop_owner_id()));

DROP POLICY IF EXISTS "vendas_update_own" ON public.vendas;
CREATE POLICY "vendas_update_own" ON public.vendas
  FOR UPDATE USING (user_id = (SELECT public.get_workshop_owner_id()))
  WITH CHECK (user_id = (SELECT public.get_workshop_owner_id()));

DROP POLICY IF EXISTS "vendas_delete_own" ON public.vendas;
CREATE POLICY "vendas_delete_own" ON public.vendas
  FOR DELETE USING (user_id = (SELECT public.get_workshop_owner_id()));

CREATE TABLE IF NOT EXISTS public.venda_itens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  venda_id UUID NOT NULL REFERENCES public.vendas(id) ON DELETE CASCADE,
  peca_id UUID REFERENCES public.pecas_estoque(id) ON DELETE SET NULL,
  codigo_sku TEXT NOT NULL DEFAULT '',
  peca_nome TEXT NOT NULL,
  quantidade INTEGER NOT NULL CHECK (quantidade > 0),
  preco_unitario NUMERIC(12, 2) NOT NULL DEFAULT 0.00 CHECK (preco_unitario >= 0),
  subtotal NUMERIC(12, 2) NOT NULL DEFAULT 0.00 CHECK (subtotal >= 0),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_venda_itens_venda ON public.venda_itens(user_id, venda_id);
CREATE INDEX IF NOT EXISTS idx_venda_itens_peca ON public.venda_itens(peca_id);

ALTER TABLE public.venda_itens ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "venda_itens_select_own" ON public.venda_itens;
CREATE POLICY "venda_itens_select_own" ON public.venda_itens
  FOR SELECT USING (user_id = (SELECT public.get_workshop_owner_id()));

DROP POLICY IF EXISTS "venda_itens_insert_own" ON public.venda_itens;
CREATE POLICY "venda_itens_insert_own" ON public.venda_itens
  FOR INSERT WITH CHECK (user_id = (SELECT public.get_workshop_owner_id()));

DROP POLICY IF EXISTS "venda_itens_delete_own" ON public.venda_itens;
CREATE POLICY "venda_itens_delete_own" ON public.venda_itens
  FOR DELETE USING (user_id = (SELECT public.get_workshop_owner_id()));

-- Vínculo da transação financeira com a venda
ALTER TABLE public.transacoes_financeiras
  ADD COLUMN IF NOT EXISTS venda_id UUID REFERENCES public.vendas(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_fin_venda_id ON public.transacoes_financeiras(venda_id);

-- RPC Atômico de Venda:
-- 1) Bloqueia a sequência da oficina via pg_advisory_xact_lock (seguro p/ 200+ lojas simultâneas)
-- 2) Dá baixa atômica em pecas_estoque (validando quantidade disponível)
-- 3) Grava public.vendas + public.venda_itens
-- 4) Lança automaticamente em transacoes_financeiras (mesmo quando vendedor for funcionário)
CREATE OR REPLACE FUNCTION public.registrar_venda_completa(
  p_cliente_id UUID DEFAULT NULL,
  p_cliente_nome TEXT DEFAULT 'Consumidor Final',
  p_cliente_telefone TEXT DEFAULT '',
  p_vendedor_id UUID DEFAULT NULL,
  p_vendedor_nome TEXT DEFAULT '',
  p_forma_pagamento TEXT DEFAULT 'PIX',
  p_desconto NUMERIC DEFAULT 0,
  p_pagamento_pendente BOOLEAN DEFAULT false,
  p_observacoes TEXT DEFAULT '',
  p_itens JSONB DEFAULT '[]'::jsonb
)
RETURNS SETOF public.vendas
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_owner UUID;
  v_next_num INTEGER;
  v_subtotal NUMERIC(12, 2) := 0.00;
  v_desconto NUMERIC(12, 2) := GREATEST(0, COALESCE(p_desconto, 0));
  v_total NUMERIC(12, 2) := 0.00;
  v_venda public.vendas%ROWTYPE;
  v_item JSONB;
  v_peca_id UUID;
  v_peca_nome TEXT;
  v_codigo_sku TEXT;
  v_qty INTEGER;
  v_preco NUMERIC(12, 2);
  v_item_sub NUMERIC(12, 2);
  v_estoque_atual INTEGER;
  v_clean_itens JSONB := '[]'::jsonb;
  v_cliente_final TEXT := COALESCE(NULLIF(trim(p_cliente_nome), ''), 'Consumidor Final');
  v_forma TEXT := COALESCE(NULLIF(trim(p_forma_pagamento), ''), 'PIX');
  v_status_venda TEXT;
  v_status_fin TEXT;
BEGIN
  IF auth.uid() IS NULL THEN
    RAISE EXCEPTION 'Usuário não autenticado.';
  END IF;

  v_owner := public.get_workshop_owner_id();
  IF v_owner IS NULL THEN
    RAISE EXCEPTION 'Oficina não identificada para o usuário atual.';
  END IF;

  IF p_itens IS NULL OR jsonb_typeof(p_itens) <> 'array' OR jsonb_array_length(p_itens) = 0 THEN
    RAISE EXCEPTION 'Adicione pelo menos 1 item para finalizar a venda.';
  END IF;

  PERFORM pg_advisory_xact_lock(hashtext(v_owner::text || ':venda'));

  SELECT COALESCE(MAX(numero_venda), 0) + 1
  INTO v_next_num
  FROM public.vendas
  WHERE user_id = v_owner;

  FOR v_item IN SELECT * FROM jsonb_array_elements(p_itens)
  LOOP
    v_peca_id := NULL;
    IF COALESCE(v_item ->> 'peca_id', '') ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$' THEN
      v_peca_id := (v_item ->> 'peca_id')::uuid;
    END IF;

    v_qty := COALESCE((v_item ->> 'quantidade')::integer, 0);
    IF v_qty <= 0 THEN
      RAISE EXCEPTION 'Quantidade de item deve ser maior que zero.';
    END IF;

    v_preco := GREATEST(0, COALESCE((v_item ->> 'preco_unitario')::numeric, 0));
    v_peca_nome := COALESCE(NULLIF(trim(v_item ->> 'nome'), ''), 'Produto');
    v_codigo_sku := COALESCE(trim(v_item ->> 'codigo_sku'), '');

    IF v_peca_id IS NOT NULL THEN
      SELECT quantidade, nome, codigo_sku
      INTO v_estoque_atual, v_peca_nome, v_codigo_sku
      FROM public.pecas_estoque
      WHERE id = v_peca_id AND user_id = v_owner
      FOR UPDATE;

      IF NOT FOUND THEN
        RAISE EXCEPTION 'Peça não encontrada no estoque da oficina.';
      END IF;

      IF v_estoque_atual < v_qty THEN
        RAISE EXCEPTION 'Estoque insuficiente para "%" (disponível: %, solicitado: %).', v_peca_nome, v_estoque_atual, v_qty;
      END IF;

      UPDATE public.pecas_estoque
      SET quantidade = quantidade - v_qty
      WHERE id = v_peca_id AND user_id = v_owner;
    END IF;

    v_item_sub := ROUND(v_preco * v_qty, 2);
    v_subtotal := v_subtotal + v_item_sub;

    v_clean_itens := v_clean_itens || jsonb_build_array(
      jsonb_build_object(
        'peca_id', v_peca_id,
        'codigo_sku', v_codigo_sku,
        'nome', v_peca_nome,
        'quantidade', v_qty,
        'preco_unitario', v_preco,
        'subtotal', v_item_sub
      )
    );
  END LOOP;

  v_total := GREATEST(0, ROUND(v_subtotal - v_desconto, 2));
  v_status_venda := CASE WHEN COALESCE(p_pagamento_pendente, false) THEN 'PENDENTE' ELSE 'CONCLUIDA' END;
  v_status_fin := CASE WHEN COALESCE(p_pagamento_pendente, false) THEN 'PENDENTE' ELSE 'PAGO' END;

  INSERT INTO public.vendas (
    user_id,
    numero_venda,
    cliente_id,
    cliente_nome,
    cliente_telefone,
    vendedor_id,
    vendedor_nome,
    forma_pagamento,
    status,
    subtotal,
    desconto,
    valor_total,
    observacoes,
    itens,
    data_venda
  ) VALUES (
    v_owner,
    v_next_num,
    p_cliente_id,
    v_cliente_final,
    COALESCE(trim(p_cliente_telefone), ''),
    p_vendedor_id,
    COALESCE(trim(p_vendedor_nome), ''),
    v_forma,
    v_status_venda,
    v_subtotal,
    v_desconto,
    v_total,
    COALESCE(trim(p_observacoes), ''),
    v_clean_itens,
    NOW()
  )
  RETURNING * INTO v_venda;

  INSERT INTO public.venda_itens (
    user_id,
    venda_id,
    peca_id,
    codigo_sku,
    peca_nome,
    quantidade,
    preco_unitario,
    subtotal
  )
  SELECT
    v_owner,
    v_venda.id,
    CASE WHEN COALESCE(x ->> 'peca_id', '') <> '' THEN (x ->> 'peca_id')::uuid ELSE NULL END,
    COALESCE(x ->> 'codigo_sku', ''),
    COALESCE(x ->> 'nome', 'Produto'),
    (x ->> 'quantidade')::integer,
    (x ->> 'preco_unitario')::numeric,
    (x ->> 'subtotal')::numeric
  FROM jsonb_array_elements(v_clean_itens) AS x;

  INSERT INTO public.transacoes_financeiras (
    user_id,
    venda_id,
    descricao,
    categoria,
    tipo,
    escopo,
    status,
    valor,
    forma_pagamento,
    data_vencimento,
    data_pagamento,
    is_conta_fixa,
    observacoes
  ) VALUES (
    v_owner,
    v_venda.id,
    'Venda #' || v_next_num || ' - ' || v_cliente_final,
    'Venda de Produtos',
    'RECEBER',
    'LOJA',
    v_status_fin,
    v_total,
    v_forma,
    CURRENT_DATE,
    CASE WHEN v_status_fin = 'PAGO' THEN CURRENT_DATE ELSE NULL END,
    false,
    'Lançamento automático PDV • Venda #' || v_next_num ||
      CASE WHEN COALESCE(trim(p_vendedor_nome), '') <> '' THEN ' • Vendedor: ' || trim(p_vendedor_nome) ELSE '' END
  );

  RETURN NEXT v_venda;
END;
$$;

CREATE OR REPLACE FUNCTION public.cancelar_venda_completa(
  p_venda_id UUID
)
RETURNS SETOF public.vendas
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_owner UUID;
  v_venda public.vendas%ROWTYPE;
  v_item RECORD;
BEGIN
  IF auth.uid() IS NULL THEN
    RAISE EXCEPTION 'Usuário não autenticado.';
  END IF;

  v_owner := public.get_workshop_owner_id();

  SELECT * INTO v_venda
  FROM public.vendas
  WHERE id = p_venda_id AND user_id = v_owner
  FOR UPDATE;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Venda não encontrada.';
  END IF;

  IF v_venda.status = 'CANCELADA' THEN
    RETURN NEXT v_venda;
    RETURN;
  END IF;

  FOR v_item IN
    SELECT peca_id, quantidade
    FROM public.venda_itens
    WHERE venda_id = p_venda_id AND user_id = v_owner AND peca_id IS NOT NULL
  LOOP
    UPDATE public.pecas_estoque
    SET quantidade = quantidade + v_item.quantidade
    WHERE id = v_item.peca_id AND user_id = v_owner;
  END LOOP;

  UPDATE public.vendas
  SET status = 'CANCELADA'
  WHERE id = p_venda_id AND user_id = v_owner
  RETURNING * INTO v_venda;

  DELETE FROM public.transacoes_financeiras
  WHERE venda_id = p_venda_id AND user_id = v_owner;

  RETURN NEXT v_venda;
END;
$$;

GRANT EXECUTE ON FUNCTION public.registrar_venda_completa(UUID, TEXT, TEXT, UUID, TEXT, TEXT, NUMERIC, BOOLEAN, TEXT, JSONB) TO authenticated;
GRANT EXECUTE ON FUNCTION public.cancelar_venda_completa(UUID) TO authenticated;

-- ============================================================================
-- 9. SUPABASE STORAGE (Fotos compactadas em WebP de OS e Componentes)
-- ============================================================================
INSERT INTO storage.buckets (id, name, public)
VALUES ('tecsec-midias', 'tecsec-midias', true)
ON CONFLICT (id) DO UPDATE SET public = true;

DROP POLICY IF EXISTS "tecsec_midias_select" ON storage.objects;
CREATE POLICY "tecsec_midias_select" ON storage.objects
  FOR SELECT USING (bucket_id = 'tecsec-midias');

DROP POLICY IF EXISTS "tecsec_midias_insert_own" ON storage.objects;
CREATE POLICY "tecsec_midias_insert_own" ON storage.objects
  FOR INSERT TO authenticated
  WITH CHECK (
    bucket_id = 'tecsec-midias'
    AND (storage.foldername(name))[1] = (SELECT public.get_workshop_owner_id()::text)
  );

DROP POLICY IF EXISTS "tecsec_midias_update_own" ON storage.objects;
CREATE POLICY "tecsec_midias_update_own" ON storage.objects
  FOR UPDATE TO authenticated
  USING (
    bucket_id = 'tecsec-midias'
    AND (storage.foldername(name))[1] = (SELECT public.get_workshop_owner_id()::text)
  );

DROP POLICY IF EXISTS "tecsec_midias_delete_own" ON storage.objects;
CREATE POLICY "tecsec_midias_delete_own" ON storage.objects
  FOR DELETE TO authenticated
  USING (
    bucket_id = 'tecsec-midias'
    AND (storage.foldername(name))[1] = (SELECT public.get_workshop_owner_id()::text)
  );
