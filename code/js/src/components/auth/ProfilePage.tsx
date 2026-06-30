import React, { useEffect, useState } from 'react';
import '../../styles/ProfilePage.css';
import { userService } from '../../services/userService';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { caseService } from '../../services/caseService';

type CurrentUser = {
  userId?: number;
  id?: number;
  username?: string;
  name?: string;
  email?: string;
};

type ProfileCase = {
  id: string;
  title: string;
  description?: string;
  status?: string;
  createdAt?: string;
};

export default function ProfilePage() {
  const { logout } = useAuth();
  const handleLogout = async () => {
    await logout();
    navigate('/initial');
  }
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [assignedCases, setAssignedCases] = useState<ProfileCase[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  const getStatusClass = (status?: string) => {
    if (!status) return 'Pendente';
    const value = String(status).toLowerCase();
    if (value === 'open' || value === 'aberto') return 'Aberto';
    if (value === 'in_progress' || value === 'in-progress' || value === 'in progress' || value === 'em progresso') return 'EmProgresso';
    if (value === 'closed' || value === 'fechado') return 'Fechado';
    return 'Pendente';
  };

  const getStatusLabel = (status?: string) => {
    if (!status) return 'Pendente';
    const value = String(status).toLowerCase();
    if (value === 'open' || value === 'aberto') return 'Aberto';
    if (value === 'in_progress' || value === 'in-progress' || value === 'in progress' || value === 'em progresso') return 'Em Progresso';
    if (value === 'closed' || value === 'fechado') return 'Fechado';
    return 'Pendente';
  };

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        const [data, cases] = await Promise.all([
          userService.getMe(),
          caseService.getAllCases(),
        ]);
        const currentUserId = String((data as any).userId ?? (data as any).id);
        const userCases = (cases as any[])
          .filter((caseItem) => {
            const caseUserId = caseItem.userId ?? caseItem.user ?? caseItem.reporter;
            return String(caseUserId) === currentUserId;
          })
          .map((caseItem) => {
            const caseId = String(caseItem.id ?? caseItem.caseId ?? '');
            return {
              id: caseId,
              title: `Caso ${caseId}`,
              description: caseItem.description,
              status: caseItem.status ?? caseItem.state,
              createdAt: caseItem.createdAt ?? caseItem.created_at,
            };
          });

        setUser(data as any);
        setAssignedCases(userCases);
      } catch (err: any) {
        setError(err?.message || 'Erro ao carregar perfil');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  if (loading) return <div className="profile-wrapper"><div className="profile-card">A carregar...</div></div>;
  if (error) return <div className="profile-wrapper"><div className="profile-card error">Erro: {error}</div></div>;

  const username = user?.username ?? user?.name ?? '-';
  const email = user?.email ?? '-';
  const openCases = assignedCases.filter((caseItem) => getStatusClass(caseItem.status) === 'Aberto').length;
  const inProgressCases = assignedCases.filter((caseItem) => getStatusClass(caseItem.status) === 'EmProgresso').length;
  const closedCases = assignedCases.filter((caseItem) => getStatusClass(caseItem.status) === 'Fechado').length;

  return (
    <div className="profile-wrapper">
      <button className="profile-back-btn" onClick={() => navigate(-1)}>Voltar</button>
      <div className="logout-wrapper">
        <button className="logout-btn" onClick={handleLogout}>Logout</button>
      </div>
      <div className="profile-shell">
        <section className="profile-card">
          <h1 className="profile-title">Perfil</h1>
          <div className="profile-row"><strong>Nome:</strong> <span>{username}</span></div>
          <div className="profile-row"><strong>Email:</strong> <span>{email}</span></div>
        </section>

        <section className="profile-summary-grid">
          <div className="profile-summary-card">
            <span>Casos em aberto</span>
            <strong>{openCases}</strong>
          </div>
          <div className="profile-summary-card">
            <span>Casos em progresso</span>
            <strong>{inProgressCases}</strong>
          </div>
          <div className="profile-summary-card">
            <span>Casos fechados</span>
            <strong>{closedCases}</strong>
          </div>
        </section>

        <section className="profile-cases-section">
          <div className="profile-section-header">
            <h2>Os meus casos</h2>
          </div>

          {assignedCases.length === 0 ? (
            <div className="profile-empty-cases">Ainda não existem casos associados ao teu perfil.</div>
          ) : (
            <div className="profile-cases-list">
              {assignedCases.map((caseItem) => (
                <div className="profile-case-card" key={caseItem.id}>
                  <div className="profile-case-header">
                    <h3>{caseItem.title}</h3>
                    <span className={`profile-case-status ${getStatusClass(caseItem.status)}`}>
                      {getStatusLabel(caseItem.status)}
                    </span>
                  </div>

                  <p>{caseItem.description || 'Sem descrição.'}</p>
                  <small>
                    Criado: {caseItem.createdAt ? new Date(caseItem.createdAt).toLocaleString() : '-'}
                  </small>

                  <button type="button" onClick={() => navigate(`/cases/${caseItem.id}`)}>
                    Aceder ao caso
                  </button>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
