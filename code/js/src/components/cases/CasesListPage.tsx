import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import '../../styles/CaseListPage.css';
import { caseService } from '../../services/caseService';
import { userService } from '../../services/userService';

// Estrutura Case - ajustar conforme o backend se necessário
export interface Case {
  id: string;
  title: string;
  description?: string;
  status?: 'Aberto' | 'Fechado' | 'Em Progresso' | string;
  reporter?: string;
  reporterName?: string;
  createdAt?: string; // ISO date
  updatedAt?: string; // ISO date
}

function CasesListPage(){
  const navigate = useNavigate();
  const { logout } = useAuth();

  const handleLogout = async () => {
    await logout();
    navigate('/initial');
  }
  const [error, setError] = useState<string | null>(null);
  const [cases, setCases] = useState<Case[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        const data = await caseService.getAllCases();
        console.debug('Raw cases data from API:', data);
        const mapped = data.map((c: any) => {
          const idStr = String(c.id ?? c.caseId ?? "");
          return {
            id: idStr,
            title: `Caso ${idStr}`,
            description: c.description,
            status: c.status ?? c.state ?? 'Desconhecido',
            reporter: c.userId ? String(c.userId) : (c.user ? String(c.user) : undefined),
            createdAt: c.createdAt ?? c.created_at ?? undefined,
            updatedAt: c.updatedAt ?? c.updated_at ?? undefined,
          };
        });

        // Remove possible duplicates by id (keep first occurrence)
        const uniqueById = Array.from(new Map(mapped.map((m) => [m.id, m])).values());

        console.debug('Mapped cases (after dedupe):', uniqueById);

        // Map reporter id -> username by fetching each user individually.
        // Note: GET /api/users requires only authentication, while listing all users may need ADMIN role.
        try {
          const reporterIds = Array.from(new Set(uniqueById.map(c => c.reporter).filter(Boolean)));
          console.debug('Reporter IDs to fetch:', reporterIds);
          const userEntries = await Promise.allSettled(
            reporterIds.map(id => userService.getUserById(Number(id)))
          );
            console.debug('Reporter IDs to fetch:', reporterIds);
            const userMap = new Map<string, string>();
            userEntries.forEach((res, idx) => {
            const id = String(reporterIds[idx]);
            if (res.status === 'fulfilled' && res.value) {
              const u = res.value as any;
              // backend returns `name` for user full name, fallback to username/email/id
              userMap.set(id, u.name || u.username || u.email || id);
            } else {
              // optional: log failed user fetch for debugging
                console.debug('Failed to load user', reporterIds[idx], res);
            }
          });
          const withReporterName = uniqueById.map(c => ({ ...c, reporterName: c.reporter ? userMap.get(c.reporter) : undefined }));
          setCases(withReporterName);
        } catch (e) {
          setCases(uniqueById);
        }
      } catch (err: any) {
        setError(err?.message || 'Erro ao carregar casos');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const getStatusClass = (status?: string) => {
    if (!status) return 'Pendente';
    const s = String(status).toLowerCase();
    if (s === 'open' || s === 'aberto') return 'Aberto';
    if (s === 'in_progress' || s === 'in-progress' || s === 'in progress' || s === 'em progresso') return 'EmProgresso';
    if (s === 'closed' || s === 'fechado') return 'Fechado';
    return 'Pendente';
  };

  const getStatusLabel = (status?: string) => {
    if (!status) return 'Pendente';
    const s = String(status).toLowerCase();
    if (s === 'open' || s === 'aberto') return 'Aberto';
    if (s === 'in_progress' || s === 'in-progress' || s === 'in progress' || s === 'em progresso') return 'Em Progresso';
    if (s === 'closed' || s === 'fechado') return 'Fechado';
    return 'Pendente';
  };

  return (
    <div className="case-list-page">
      <div className="logout-wrapper">
        <button className="logout-btn" onClick={handleLogout}>Logout</button>
      </div>
      <button className="page-btn secondary home-btn" onClick={() => navigate('/home')}>Voltar</button>
      <div className="case-list-header">
        <div>
          <h1 className="case-list-title">Casos</h1>
          <p className="case-list-subtitle">Acompanhe seus casos e detalhes de forma rápida.</p>
        </div>
      </div>

      {error && <p style={{ color: 'red' }}>Erro: {error}</p>}

      {loading && <p>Carregando casos...</p>}
      {!loading && !error && cases.length === 0 && <p>Nenhum caso encontrado.</p>}

      <div className="case-list-grid">
        {cases.map((c) => (
          <div key={c.id} className="case-card">
            <div className="case-card-header" style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <h2 className="case-title">{c.title}</h2>
              <span className={`case-status ${getStatusClass(c.status)}`}>{getStatusLabel(c.status)}</span>
            </div>
            <p className="case-meta">{c.description || '—'}</p>
            <div className="case-meta">Averiguador: {(c as any).reporterName || c.reporter || '—'}</div>
            <div className="case-meta">Criado: {c.createdAt ? new Date(c.createdAt).toLocaleString() : '—'}</div>
            <div className="case-meta">Atualizado: {c.updatedAt ? new Date(c.updatedAt).toLocaleString() : '—'}</div>
            <div className="case-footer">
              <button className="btn-link" onClick={() => navigate(`/cases/${c.id}`)}>Aceder</button>
            </div>
          </div>
        ))}
      </div>
        <div className="case-list-actions">
          <button className="page-btn" onClick={() => navigate('/cases/create')}>Criar Novo Caso</button>
        </div>
    </div>
    
  );
};

export default CasesListPage;
