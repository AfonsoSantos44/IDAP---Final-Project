import React, { useState} from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import '../../styles/CaseListPage.css';

// Estrutura Case - ajustar conforme o backend se necessário
export interface Case {
  id: string;
  title: string;
  description?: string;
  status?: 'Aberto' | 'Fechado' | 'Em Progresso' | string;
  reporter?: string;
  createdAt?: string; // ISO date
  updatedAt?: string; // ISO date
}

function CasesListPage(){
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const cases = [{
    id: '1',
    title: 'Caso de teste 1',
    description: 'Descrição do caso de teste 1',
    status: 'Aberto',
    reporter: 'Averiguador 1',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  }];

  const getStatusClass = (status?: string) => {
    return status ? status.toLowerCase().replace(/\s+/g, '-') : 'pending';
  };

  return (
    <div className="case-list-page">
      <div className="case-list-header">
        <div>
          <h1 className="case-list-title">Lista de Casos</h1>
          <p className="case-list-subtitle">Acompanhe seus casos e detalhes de forma rápida.</p>
        </div>
      </div>

      {error && <p style={{ color: 'red' }}>Erro: {error}</p>}

      {!error && cases.length === 0 && <p>Nenhum caso encontrado.</p>}

      <div className="case-list-grid">
        {cases.map((c) => (
          <div key={c.id} className="case-card">
            <div className="case-card-header" style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <h2 className="case-title">{c.title}</h2>
              <span className={`case-status ${getStatusClass(c.status)}`}>{c.status ?? 'Desconhecido'}</span>
            </div>
            <p className="case-meta">{c.description || '—'}</p>
            <div className="case-meta">Averiguador: {c.reporter || '—'}</div>
            <div className="case-meta">Criado: {c.createdAt ? new Date(c.createdAt).toLocaleString() : '—'}</div>
            <div className="case-meta">Atualizado: {c.updatedAt ? new Date(c.updatedAt).toLocaleString() : '—'}</div>
            <div className="case-footer">
              <button className="btn-link" onClick={() => navigate(`/cases/${c.id}`)}>Aceder</button>
              <span className="case-meta">ID: {c.id}</span>
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
