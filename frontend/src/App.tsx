import React, { useState, useRef, useEffect } from 'react';
import { Send, Bot, User, Sparkles, Sidebar as SidebarIcon, Plus, Settings, Trash2 } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { cn } from './lib/utils';
import axios from 'axios';

interface Message {
  role: 'user' | 'assistant';
  content: string;
}

interface ChatSession {
  id: string;
  title: string;
  messages: Message[];
  createdAt: number;
  updatedAt: number;
}

const STORAGE_KEY = 'chat-sessions';
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const ChatApp = () => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);
  const [chatSessions, setChatSessions] = useState<ChatSession[]>([]);
  const [currentSessionId, setCurrentSessionId] = useState<string | null>(null);
  const [isBackendConnected, setIsBackendConnected] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // 백엔드 연결 상태 확인
  useEffect(() => {
    const checkBackendHealth = async () => {
      try {
        const response = await axios.get(`${API_BASE_URL}/actuator/health`, {
          timeout: 3000
        });
        console.log('Health check response:', response.data);
        setIsBackendConnected(response.data.status === 'UP');
      } catch (error) {
        console.error('Health check failed:', error);
        setIsBackendConnected(false);
      }
    };

    // 초기 체크
    checkBackendHealth();

    // 30초마다 체크
    const interval = setInterval(checkBackendHealth, 30000);

    return () => clearInterval(interval);
  }, []);

  // 로컬 스토리지에서 채팅 세션 불러오기
  useEffect(() => {
    const savedSessions = localStorage.getItem(STORAGE_KEY);
    if (savedSessions) {
      const sessions = JSON.parse(savedSessions) as ChatSession[];
      setChatSessions(sessions.sort((a, b) => b.updatedAt - a.updatedAt));
    }
  }, []);

  // 현재 세션의 메시지가 변경될 때마다 로컬 스토리지에 저장
  useEffect(() => {
    if (currentSessionId && messages.length > 0) {
      const updatedSessions = chatSessions.map(session => 
        session.id === currentSessionId 
          ? { 
              ...session, 
              messages, 
              updatedAt: Date.now(),
              title: messages[0]?.content.slice(0, 50) || 'New Chat'
            }
          : session
      );
      setChatSessions(updatedSessions.sort((a, b) => b.updatedAt - a.updatedAt));
      localStorage.setItem(STORAGE_KEY, JSON.stringify(updatedSessions));
    }
  }, [messages, currentSessionId]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // 새 채팅 시작
  const startNewChat = () => {
    const newSession: ChatSession = {
      id: Date.now().toString(),
      title: 'New Chat',
      messages: [],
      createdAt: Date.now(),
      updatedAt: Date.now(),
    };
    const updatedSessions = [newSession, ...chatSessions];
    setChatSessions(updatedSessions);
    setCurrentSessionId(newSession.id);
    setMessages([]);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(updatedSessions));
  };

  // 채팅 세션 선택
  const loadChatSession = (sessionId: string) => {
    const session = chatSessions.find(s => s.id === sessionId);
    if (session) {
      setCurrentSessionId(sessionId);
      setMessages(session.messages);
    }
  };

  // 채팅 세션 삭제
  const deleteChatSession = (sessionId: string, e: React.MouseEvent) => {
    e.stopPropagation(); // 클릭 이벤트가 부모로 전파되지 않도록
    const updatedSessions = chatSessions.filter(s => s.id !== sessionId);
    setChatSessions(updatedSessions);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(updatedSessions));
    
    // 현재 선택된 채팅이 삭제되면 빈 화면으로
    if (currentSessionId === sessionId) {
      setCurrentSessionId(null);
      setMessages([]);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() || isLoading) return;

    // 현재 세션이 없으면 새로 생성
    if (!currentSessionId) {
      startNewChat();
    }

    const userMessage: Message = { role: 'user', content: input };
    setMessages((prev) => [...prev, userMessage]);
    setInput('');
    setIsLoading(true);

    try {
      const response = await axios.post(`${API_BASE_URL}/api/chat`, {
        messages: [...messages, userMessage],
      });

      setMessages((prev) => [...prev, response.data.message]);
    } catch (error) {
      console.error('Chat Error:', error);
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: '죄송합니다. 오류가 발생했습니다. API 키가 설정되어 있는지 확인해 주세요.' },
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex h-screen w-full bg-[#0a0a0a] text-gray-100 overflow-hidden">
      {/* Sidebar */}
      <AnimatePresence mode="wait">
        {isSidebarOpen && (
          <motion.div
            initial={{ width: 0, opacity: 0 }}
            animate={{ width: 300, opacity: 1 }}
            exit={{ width: 0, opacity: 0 }}
            className="h-full bg-[#111111] border-r border-white/5 flex flex-col"
          >
            <div className="p-4 flex items-center justify-between">
              <button
                onClick={startNewChat}
                className="flex items-center gap-2 px-4 py-2 rounded-lg bg-white/5 hover:bg-white/10 transition-colors w-full border border-white/5"
              >
                <Plus size={18} />
                <span className="text-sm font-medium">New Chat</span>
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-4 space-y-2">
              <div className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">Recent Chats</div>
              {chatSessions.length === 0 ? (
                <div className="text-xs text-gray-600 text-center py-4">
                  채팅을 시작하면 여기에 표시됩니다
                </div>
              ) : (
                chatSessions.map((session) => (
                  <div
                    key={session.id}
                    onClick={() => loadChatSession(session.id)}
                    className={cn(
                      "p-3 rounded-lg text-sm cursor-pointer transition-colors border group relative",
                      currentSessionId === session.id
                        ? "bg-white/10 border-white/20"
                        : "bg-white/5 border-white/10 hover:bg-white/10"
                    )}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex-1 min-w-0">
                        <p className="truncate">{session.title}</p>
                        <p className="text-xs text-gray-500 mt-1">
                          {new Date(session.updatedAt).toLocaleDateString('ko-KR', {
                            month: 'short',
                            day: 'numeric',
                            hour: '2-digit',
                            minute: '2-digit'
                          })}
                        </p>
                      </div>
                      <button
                        onClick={(e) => deleteChatSession(session.id, e)}
                        className="opacity-0 group-hover:opacity-100 p-1.5 rounded-md hover:bg-red-500/20 text-gray-400 hover:text-red-400 transition-all"
                        title="삭제"
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>

            <div className="p-4 border-t border-white/5 flex items-center gap-3">
              <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-purple-500 to-blue-500 flex items-center justify-center">
                <User size={16} />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium truncate">User</p>
              </div>
              <button className="text-gray-400 hover:text-white transition-colors">
                <Settings size={18} />
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Main Content */}
      <div className="flex-1 flex flex-col relative">
        <header className="h-16 flex items-center justify-between px-6 border-b border-white/5 bg-[#0a0a0a]/80 backdrop-blur-md sticky top-0 z-10">
          <div className="flex items-center gap-4">
            <button
              onClick={() => setIsSidebarOpen(!isSidebarOpen)}
              className="p-2 hover:bg-white/5 rounded-lg transition-colors"
            >
              <SidebarIcon size={20} className="text-gray-400" />
            </button>
            <div className="flex items-center gap-2">
              <div className="p-1.5 rounded-lg bg-purple-500/20 text-purple-400">
                <Sparkles size={18} />
              </div>
              <h1 className="font-semibold">Interactive AI Agent</h1>
            </div>
          </div>
          <div className={cn(
            "flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-medium border transition-colors",
            isBackendConnected
              ? "bg-green-500/10 text-green-400 border-green-500/20"
              : "bg-red-500/10 text-red-400 border-red-500/20"
          )}>
            <div className={cn(
              "w-2 h-2 rounded-full",
              isBackendConnected ? "bg-green-500 animate-pulse" : "bg-red-500"
            )} />
            {isBackendConnected ? "Groq Llama 3.3 Connected" : "Backend Disconnected"}
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-6 md:px-20 lg:px-40 space-y-8 scrollbar-hide">
          {messages.length === 0 ? (
            <div className="h-full flex flex-col items-center justify-center text-center space-y-4 opacity-50">
              <Bot size={64} className="text-purple-400" />
              <div>
                <h2 className="text-2xl font-bold">How can I help you today?</h2>
                <p className="text-gray-400 mt-2">Start a conversation with your Groq-powered AI Agent.</p>
              </div>
            </div>
          ) : (
            messages.map((msg, i) => (
              <motion.div
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                key={i}
                className={cn(
                  "flex gap-4",
                  msg.role === 'user' ? "flex-row-reverse" : ""
                )}
              >
                <div className={cn(
                  "w-10 h-10 rounded-xl flex items-center justify-center shrink-0",
                  msg.role === 'user'
                    ? "bg-blue-500/20 text-blue-400 border border-blue-500/20"
                    : "bg-purple-500/20 text-purple-400 border border-purple-500/20"
                )}>
                  {msg.role === 'user' ? <User size={20} /> : <Bot size={20} />}
                </div>
                <div className={cn(
                  "max-w-[80%] rounded-2xl p-4 shadow-xl",
                  msg.role === 'user'
                    ? "bg-blue-600/10 border border-blue-500/20 text-blue-50"
                    : "bg-white/5 border border-white/10 text-gray-100"
                )}>
                  <p className="leading-relaxed whitespace-pre-wrap">{msg.content}</p>
                </div>
              </motion.div>
            ))
          )}
          {isLoading && (
            <div className="flex gap-4">
              <div className="w-10 h-10 rounded-xl flex items-center justify-center shrink-0 bg-purple-500/20 text-purple-400 border border-purple-500/20">
                <Bot size={20} />
              </div>
              <div className="bg-white/5 border border-white/10 rounded-2xl p-4 flex items-center gap-2">
                <div className="w-2 h-2 rounded-full bg-purple-400 animate-bounce" style={{ animationDelay: '0ms' }} />
                <div className="w-2 h-2 rounded-full bg-purple-400 animate-bounce" style={{ animationDelay: '150ms' }} />
                <div className="w-2 h-2 rounded-full bg-purple-400 animate-bounce" style={{ animationDelay: '300ms' }} />
              </div>
            </div>
          )}
          <div ref={messagesEndRef} />
        </main>

        <footer className="p-4 md:px-20 lg:px-40 bg-gradient-to-t from-[#0a0a0a] via-[#0a0a0a] to-transparent">
          <form onSubmit={handleSubmit} className="relative group">
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Message AI Agent..."
              className="w-full bg-[#1a1a1a] border border-white/10 rounded-2xl py-4 pl-6 pr-14 focus:outline-none focus:ring-2 focus:ring-purple-500/50 transition-all placeholder:text-gray-600 group-hover:border-white/20 shadow-2xl"
            />
            <button
              type="submit"
              disabled={isLoading || !input.trim()}
              className="absolute right-3 top-1/2 -translate-y-1/2 p-2.5 rounded-xl bg-purple-500 text-white hover:bg-purple-600 disabled:opacity-50 disabled:hover:bg-purple-500 transition-all shadow-lg shadow-purple-500/20"
            >
              <Send size={20} />
            </button>
          </form>
          <p className="text-[10px] text-center mt-3 text-gray-600 uppercase tracking-widest font-medium">
            Powered by Groq • Llama 3.3 70B
          </p>
        </footer>
      </div>
    </div>
  );
};

export default ChatApp;
