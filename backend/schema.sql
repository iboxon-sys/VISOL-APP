-- =========================================================
-- VISOL MESSAGING & REAL-TIME CHAT DATABASE SCHEMA
-- PostgreSQL / Supabase / Neon / CockroachDB compatible
-- =========================================================

-- 1. Users Table (Foydalanuvchilar)
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    phone VARCHAR(32),
    gender VARCHAR(16),
    avatar_url TEXT,
    fcm_token TEXT,
    is_online BOOLEAN DEFAULT FALSE,
    last_seen TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Messages Table (Shaxsiy Xabarlar)
-- Talab bo'yicha ustunlar:
-- id, sender_id, receiver_id, message, created_at, is_read
CREATE TABLE IF NOT EXISTS messages (
    id BIGSERIAL PRIMARY KEY,
    sender_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN DEFAULT FALSE
);

-- 3. High Performance Indexes (Tezkor indekslar)
-- 2 foydalanuvchi o'rtasidagi suhbatni izolyatsiyalangan holda bir lahzada yuklash:
CREATE INDEX IF NOT EXISTS idx_messages_conversation 
ON messages (LEAST(sender_id, receiver_id), GREATEST(sender_id, receiver_id), created_at ASC);

-- Foydalanuvchining o'qilmagan xabarlarini tezkor hisoblash:
CREATE INDEX IF NOT EXISTS idx_messages_unread 
ON messages (receiver_id, is_read) 
WHERE is_read = FALSE;

-- Foydalanuvchi so'nggi xabarlarini saralash:
CREATE INDEX IF NOT EXISTS idx_messages_sender_receiver 
ON messages (sender_id, receiver_id, created_at DESC);
