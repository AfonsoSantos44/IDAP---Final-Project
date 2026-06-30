import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import '../../styles/CaseListPage.css';
import { caseService } from '../../services/caseService';
import { userService } from '../../services/userService';

export interface Case {
  id: string;
  title: string;
  description?: string;
  status?: 'Aberto' | 'Fechado' | 'Em Progresso' | string;
  reporter?: string;
  reporterName?: string;
  createdAt?: string;
}

function CasesListPage() {
  const navigate = useNavigate();
  const { logout, userId: currentUserId, isLoading: authLoading } = useAuth();

  const [error, setError] = useState<string | null>(null);
  const [cases, setCases] = useState<Case[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [isAdmin, setIsAdmin] = useState<boolean>(false);
  const [users, setUsers] = useState<{ id: number; username?: string; email?: string }[]>([]);
  const [selectedUserFilter, setSelectedUserFilter] = useState<string>('all');
  const [isUserFilterOpen, setIsUserFilterOpen] = useState(false);
  const userFilterRef = useRef<HTMLDivElement | null>(null);

  const handleLogout = async () => {
    await logout();
    navigate('/initial');
  };

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        let admin = false;

        try {
          const allUsers = await userService.getAllUsers();
          admin = true;
          setUsers(
            allUsers
              .map((u: any) => ({
                id: Number(u.id ?? u.userId),
                username: u.username,
                email: u.email,
              }))
              .filter((u) => Number.isFinite(u.id))
          );
        } catch {
          admin = false;
          setUsers([]);
        }

        setIsAdmin(admin);

        const data = await caseService.getAllCases();
        const mapped = data.sort((a, b) => a.id - b.id).map((c: any) => {
          const idStr = String(c.id ?? c.caseId ?? '');

          return {
            id: idStr,
            title: `Caso ${idStr}`,
            description: c.description,
            status: c.status ?? c.state ?? 'Desconhecido',
            reporter: c.userId ? String(c.userId) : c.user ? String(c.user) : undefined,
            createdAt: c.createdAt ?? c.created_at ?? undefined,
          };
        });

        const uniqueById = Array.from(new Map(mapped.map((m) => [m.id, m])).values());

        try {
          const reporterIds = Array.from(new Set(uniqueById.map((c) => c.reporter).filter(Boolean)));
          const userEntries = await Promise.allSettled(
            reporterIds.map((id) => userService.getUserById(Number(id)))
          );
          const userMap = new Map<string, string>();

          userEntries.forEach((res, idx) => {
            const id = String(reporterIds[idx]);

            if (res.status === 'fulfilled' && res.value) {
              const u = res.value as any;
              userMap.set(id, u.name || u.username || u.email || id);
            } else {
              console.debug('Failed to load user', reporterIds[idx], res);
            }
          });

          let withReporterName = uniqueById.map((c) => ({
            ...c,
            reporterName: c.reporter ? userMap.get(c.reporter) : undefined,
          }));

          if (!admin && currentUserId) {
            withReporterName = withReporterName.filter((c) => String(c.reporter) === String(currentUserId));
          }

          setCases(withReporterName);
        } catch {
          let base = uniqueById;

          if (!admin && currentUserId) {
            base = base.filter((c) => String(c.reporter) === String(currentUserId));
          }

          setCases(base);
        }
      } catch (err: any) {
        setError(err?.message || 'Erro ao carregar casos');
      } finally {
        setLoading(false);
      }
    };

    if (!authLoading) load();
  }, [authLoading, currentUserId]);

  useEffect(() => {
    const closeUserFilter = (event: MouseEvent) => {
      if (!userFilterRef.current?.contains(event.target as Node)) {
        setIsUserFilterOpen(false);
      }
    };

    document.addEventListener('mousedown', closeUserFilter);
    return () => document.removeEventListener('mousedown', closeUserFilter);
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

  const visibleCases = cases.filter((c) => (
    selectedUserFilter === 'all' || String(c.reporter) === String(selectedUserFilter)
  ));

  const userFilterOptions = [
    { value: 'all', label: 'Todos' },
    ...users.map((u) => ({
      value: String(u.id),
      label: u.username || u.email || String(u.id),
    })),
  ];

  const selectedUserLabel = userFilterOptions.find((option) => option.value === selectedUserFilter)?.label || 'Todos';

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

        {isAdmin && (
          <div className="case-list-grid-controls">
            <div className="user-filter-control">
              <label id="case-user-filter-label">Filtrar por utilizador:</label>
              <div className="user-filter-dropdown" ref={userFilterRef}>
                <button
                  type="button"
                  className={`user-filter-trigger ${isUserFilterOpen ? 'open' : ''}`}
                  aria-haspopup="listbox"
                  aria-expanded={isUserFilterOpen}
                  aria-labelledby="case-user-filter-label"
                  onClick={() => setIsUserFilterOpen((open) => !open)}
                >
                  <span>{selectedUserLabel}</span>
                  <span className="user-filter-chevron">▾</span>
                </button>

                {isUserFilterOpen && (
                  <div className="user-filter-menu" role="listbox" aria-labelledby="case-user-filter-label">
                    {userFilterOptions.map((option) => (
                      <button
                        key={option.value}
                        type="button"
                        className={option.value === selectedUserFilter ? 'selected' : ''}
                        role="option"
                        aria-selected={option.value === selectedUserFilter}
                        onClick={() => {
                          setSelectedUserFilter(option.value);
                          setIsUserFilterOpen(false);
                        }}
                      >
                        {option.label}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </div>

      {error && <p style={{ color: 'red' }}>Erro: {error}</p>}
      {loading && <p>Carregando casos...</p>}
      {!loading && !error && visibleCases.length === 0 && <p>Nenhum caso encontrado.</p>}

      <div className="case-list-grid">
        {visibleCases.map((c) => (
          <div key={c.id} className="case-card">
            <div className="case-card-header" style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <h2 className="case-title">{c.title}</h2>
              <span className={`case-status ${getStatusClass(c.status)}`}>
                {getStatusLabel(c.status)}
              </span>
            </div>

            <p className="case-meta">{c.description || '-'}</p>
            <div className="case-meta">Averiguador: {c.reporterName || c.reporter || '-'}</div>
            <div className="case-meta">Criado: {c.createdAt ? new Date(c.createdAt).toLocaleString() : '-'}</div>

            <div className="case-footer">
              <button className="btn-link" onClick={() => navigate(`/cases/${c.id}`)}>
                Aceder
              </button>
            </div>
          </div>
        ))}
      </div>

      <div className="case-list-actions">
        {isAdmin && <button className="page-btn" onClick={() => navigate('/cases/create')}>Criar Novo Caso</button>}
      </div>
    </div>
  );
}

export default CasesListPage;
