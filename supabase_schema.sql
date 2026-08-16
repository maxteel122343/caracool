-- ==============================================================================
-- CARA DE PAÇOCA - SCHEMA SUPABASE COMPLETO (POSTGRESQL)
-- Execute este script no SQL Editor do seu Dashboard do Supabase (supabase.com)
-- ==============================================================================

-- 1. TABELA DE USUÁRIOS E PERFIS
CREATE TABLE IF NOT EXISTS public.users (
    "userId" TEXT PRIMARY KEY,
    "email" TEXT,
    "userName" TEXT NOT NULL DEFAULT 'Você (Cara de Paçoca)',
    "userAvatarEmoji" TEXT DEFAULT '🥜',
    "userProfilePhotoUri" TEXT,
    "photoBase64" TEXT,
    "selectedThemeId" TEXT DEFAULT 'original',
    "unlockAudioType" TEXT DEFAULT 'ai_phrase',
    "isAiConversationalEnabled" BOOLEAN DEFAULT true,
    "lastUnlockTimestamp" BIGINT,
    "totalUnlocksToday" INTEGER DEFAULT 0,
    "updatedAt" BIGINT DEFAULT (EXTRACT(epoch FROM now()) * 1000)::BIGINT,
    "created_at" TIMESTAMPTZ DEFAULT now()
);

-- 2. TABELA DO FEED DA COMUNIDADE (POSTS COM FOTO E MOLDURA)
CREATE TABLE IF NOT EXISTS public.community_posts (
    "id" BIGINT PRIMARY KEY,
    "userId" TEXT NOT NULL,
    "authorName" TEXT NOT NULL DEFAULT 'Membro Paçoca',
    "authorAvatarEmoji" TEXT DEFAULT '🥜',
    "authorAvatarUri" TEXT,
    "photoUri" TEXT,
    "photoBase64" TEXT,
    "presetImageKey" TEXT,
    "caption" TEXT DEFAULT '',
    "unlockCount" INTEGER DEFAULT 1,
    "timestamp" BIGINT NOT NULL DEFAULT (EXTRACT(epoch FROM now()) * 1000)::BIGINT,
    "likesCount" INTEGER DEFAULT 0,
    "loveCount" INTEGER DEFAULT 0,
    "laughCount" INTEGER DEFAULT 0,
    "pacocaCount" INTEGER DEFAULT 0,
    "fireCount" INTEGER DEFAULT 0,
    "wowCount" INTEGER DEFAULT 0,
    "themeTag" TEXT DEFAULT 'Original',
    "commentsCount" INTEGER DEFAULT 0,
    "wallpaperSetCount" INTEGER DEFAULT 0,
    "isGuest" BOOLEAN DEFAULT false,
    "created_at" TIMESTAMPTZ DEFAULT now()
);

-- 3. TABELA DE COMENTÁRIOS NOS POSTS
CREATE TABLE IF NOT EXISTS public.community_comments (
    "id" BIGINT PRIMARY KEY,
    "postId" BIGINT NOT NULL REFERENCES public.community_posts("id") ON DELETE CASCADE,
    "userId" TEXT NOT NULL,
    "authorName" TEXT NOT NULL DEFAULT 'Amigo Paçoca',
    "authorAvatarEmoji" TEXT DEFAULT '🥜',
    "authorAvatarUri" TEXT,
    "text" TEXT NOT NULL,
    "timestamp" BIGINT NOT NULL DEFAULT (EXTRACT(epoch FROM now()) * 1000)::BIGINT,
    "isGuest" BOOLEAN DEFAULT false,
    "created_at" TIMESTAMPTZ DEFAULT now()
);

-- 4. TABELA DE REAÇÕES / CURTIDAS
CREATE TABLE IF NOT EXISTS public.community_reactions (
    "id" BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    "postId" BIGINT NOT NULL REFERENCES public.community_posts("id") ON DELETE CASCADE,
    "userId" TEXT NOT NULL,
    "userName" TEXT NOT NULL DEFAULT 'Amigo',
    "reaction" TEXT NOT NULL DEFAULT '🥜',
    "timestamp" BIGINT NOT NULL DEFAULT (EXTRACT(epoch FROM now()) * 1000)::BIGINT,
    CONSTRAINT unique_post_user_reaction UNIQUE ("postId", "userId")
);

-- 5. TABELA DE RANKING GLOBAL (CARA DE PAÇOCA & MODO CARA DE CU)
CREATE TABLE IF NOT EXISTS public.community_rankings (
    "userId" TEXT PRIMARY KEY,
    "name" TEXT NOT NULL,
    "avatarEmoji" TEXT DEFAULT '🥜',
    "photoUri" TEXT,
    "unlockCount" INTEGER DEFAULT 0,
    "isKool" BOOLEAN DEFAULT false,
    "lastActive" BIGINT DEFAULT (EXTRACT(epoch FROM now()) * 1000)::BIGINT
);

-- 6. TABELA DE LOGS DE DESBLOQUEIOS
CREATE TABLE IF NOT EXISTS public.unlock_logs (
    "id" BIGINT PRIMARY KEY,
    "userId" TEXT NOT NULL,
    "dateString" TEXT NOT NULL,
    "unlockNumberToday" INTEGER NOT NULL,
    "phraseSpoken" TEXT,
    "themeUsed" TEXT,
    "timestamp" BIGINT NOT NULL DEFAULT (EXTRACT(epoch FROM now()) * 1000)::BIGINT,
    "created_at" TIMESTAMPTZ DEFAULT now()
);

-- 7. TABELA DE PROBES DE TESTE / DIAGNÓSTICO
CREATE TABLE IF NOT EXISTS public.system_probes (
    "id" BIGINT PRIMARY KEY,
    "userId" TEXT,
    "authorName" TEXT,
    "caption" TEXT,
    "timestamp" BIGINT,
    "isProbe" BOOLEAN DEFAULT true,
    "themeTag" TEXT,
    "created_at" TIMESTAMPTZ DEFAULT now()
);

-- ÍNDICES PARA ALTA PERFORMANCE
CREATE INDEX IF NOT EXISTS idx_posts_timestamp ON public.community_posts("timestamp" DESC);
CREATE INDEX IF NOT EXISTS idx_comments_post_time ON public.community_comments("postId", "timestamp" ASC);
CREATE INDEX IF NOT EXISTS idx_rankings_unlocks ON public.community_rankings("unlockCount" DESC);
CREATE INDEX IF NOT EXISTS idx_unlock_logs_user_date ON public.unlock_logs("userId", "dateString");

-- HABILITAR ROW LEVEL SECURITY (RLS)
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.community_posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.community_comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.community_reactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.community_rankings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.unlock_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.system_probes ENABLE ROW LEVEL SECURITY;

-- POLÍTICAS DE ACESSO LIVRE (PÚBLICO / ANON / AUTHENTICATED) PARA O APP MOBILE
-- Permite leitura e gravação transparente tanto para visitantes (Modo Convidado) quanto para contas logadas
DROP POLICY IF EXISTS "Public read users" ON public.users;
CREATE POLICY "Public read users" ON public.users FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public write users" ON public.users;
CREATE POLICY "Public write users" ON public.users FOR ALL USING (true);

DROP POLICY IF EXISTS "Public read posts" ON public.community_posts;
CREATE POLICY "Public read posts" ON public.community_posts FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public write posts" ON public.community_posts;
CREATE POLICY "Public write posts" ON public.community_posts FOR ALL USING (true);

DROP POLICY IF EXISTS "Public read comments" ON public.community_comments;
CREATE POLICY "Public read comments" ON public.community_comments FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public write comments" ON public.community_comments;
CREATE POLICY "Public write comments" ON public.community_comments FOR ALL USING (true);

DROP POLICY IF EXISTS "Public read reactions" ON public.community_reactions;
CREATE POLICY "Public read reactions" ON public.community_reactions FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public write reactions" ON public.community_reactions;
CREATE POLICY "Public write reactions" ON public.community_reactions FOR ALL USING (true);

DROP POLICY IF EXISTS "Public read rankings" ON public.community_rankings;
CREATE POLICY "Public read rankings" ON public.community_rankings FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public write rankings" ON public.community_rankings;
CREATE POLICY "Public write rankings" ON public.community_rankings FOR ALL USING (true);

DROP POLICY IF EXISTS "Public read unlock_logs" ON public.unlock_logs;
CREATE POLICY "Public read unlock_logs" ON public.unlock_logs FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public write unlock_logs" ON public.unlock_logs FOR ALL USING (true);

DROP POLICY IF EXISTS "Public write probes" ON public.system_probes;
CREATE POLICY "Public write probes" ON public.system_probes FOR ALL USING (true);

-- HABILITAR SUPABASE REALTIME NAS TABELAS DO FEED
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime') THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.community_posts;
        ALTER PUBLICATION supabase_realtime ADD TABLE public.community_comments;
        ALTER PUBLICATION supabase_realtime ADD TABLE public.community_rankings;
    END IF;
EXCEPTION WHEN OTHERS THEN
    -- Realtime table publication already added or not supported in local schema
    NULL;
END $$;
