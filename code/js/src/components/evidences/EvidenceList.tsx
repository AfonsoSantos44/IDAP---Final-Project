import React, { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { evidenceService } from '../../services/evidenceService';
import { caseService } from '../../services/caseService';
import { userService } from '../../services/userService';
import analysisService from '../../services/analysisService';
import '../../styles/CaseListPage.css';
import '../../styles/EvidenceList.css';
import { useAuth } from '../../context/AuthContext';

interface Evidence {
  evidenceId?: number;
  caseId?: number;
  evidenceType?: string;
  evidenceDescription?: string;
  uploadedBy?: number;
  uploadedAt?: string;
}

export default function EvidenceList() {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const { search } = useLocation();
  const params = new URLSearchParams(search);
  const queryCaseId = params.get('caseId');
  const routeParams = useParams();
  const routeCaseId = routeParams.caseId;

  const initialCaseId = routeCaseId ?? queryCaseId;

  const [caseId, setCaseId] = useState<number | null>(initialCaseId ? Number(initialCaseId) : null);
  const [cases, setCases] = useState<any[]>([]);
  const [evidences, setEvidences] = useState<Evidence[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [filterType, setFilterType] = useState<string>('');
  const [uploaderNames, setUploaderNames] = useState<Record<string, string>>({});

  const evidenceTypes = useMemo(() => {
    const s = new Set<string>();
    evidences.forEach(e => { if (e.evidenceType) s.add(e.evidenceType); });
    return Array.from(s).sort();
  }, [evidences]);

  useEffect(() => {
    const loadCases = async () => {
      try {
        const cs = await caseService.getAllCases();
        setCases(cs as any[]);
      } catch (e) {
        // ignore — we can still allow manual id
      }
    };
    loadCases();
  }, []);

  useEffect(() => {
    const load = async () => {
      if (!caseId) return;
      setLoading(true);
      setError(null);
      try {
        const list = await evidenceService.listCaseEvidence(caseId);
        const sorted = [...(list || [])].sort(
          (a, b) => Number(a.evidenceId ?? 0) - Number(b.evidenceId ?? 0)
        );
        setEvidences(sorted);

        const uploaderIds = Array.from(
          new Set(
            sorted
              .map((ev) => ev.uploadedBy)
              .filter((id): id is number => id !== undefined && id !== null)
          )
        );

        if (uploaderIds.length > 0) {
          const results = await Promise.allSettled(
            uploaderIds.map((id) => userService.getUserById(id))
          );
          const namesMap: Record<string, string> = {};

          results.forEach((result, index) => {
            if (result.status === 'fulfilled' && result.value) {
              const u = result.value as any;
              namesMap[String(uploaderIds[index])] =
                u.name || u.username || u.email || String(uploaderIds[index]);
            }
          });

          setUploaderNames(namesMap);
        } else {
          setUploaderNames({});
        }
      } catch (err: any) {
        setError(err?.message || 'Erro ao carregar evidências');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [caseId]);

  const handleDelete = async (evidenceId?: number) => {
    if (!evidenceId) return;
    if (!confirm('Tem a certeza que deseja apagar esta evidência?')) return;
    try {
      await evidenceService.deleteEvidence(evidenceId);
      setEvidences(e => e.filter(x => x.evidenceId !== evidenceId));
    } catch (e: any) {
      alert('Erro ao apagar: ' + (e?.message || ''));
    }
  };

  const handleAnalyze = async () => {
    if (!caseId) return;

    try {
      const analyses = await analysisService.listCaseAnalyses(caseId);

      if (analyses.length > 0 && analyses[0].analysisId) {
        navigate(`/cases/${caseId}/analysis/${analyses[0].analysisId}`);
        return;
      }

      const analysis = await analysisService.createCaseAnalysis(caseId);
      navigate(`/cases/${caseId}/analysis/${analysis.analysisId}`);
    } catch (err: any) {
      alert(err?.message || 'Erro ao criar análise');
    }
  };

  const handleLogout = async () => {
    await logout();
    navigate('/initial');
  };

  return (
    <div className="case-list-page" style={{ position: 'relative' }}>
      <div className="logout-wrapper">
        <button className="logout-btn" onClick={handleLogout}>Logout</button>
      </div>
      <div className="back-container back-outside">
        <button className="page-btn secondary back-btn" onClick={() => navigate(caseId ? `/cases/${caseId}` : '/cases')}>Voltar</button>
      </div>
      <div className="case-list-header">
        <div>
          <h1 className="case-list-title">Evidências</h1>
          <p className="case-list-subtitle">Lista de evidências do caso {caseId ?? '—'}</p>
        </div>
      </div>

      <div style={{ marginBottom: 12, display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
        <button onClick={() => navigate(caseId ? `/cases/${caseId}/evidences/create` : '/cases')} className="btn-link">Adicionar Evidência</button>

        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 8 }}>
          <label style={{ fontWeight: 600 }}>Filtrar por tipo:</label>
          <select value={filterType} onChange={e => setFilterType(e.target.value)} style={{ padding: '8px 10px', borderRadius: 8 }}>
            <option value="">Todos</option>
            {evidenceTypes.map(t => <option key={t} value={t}>{t}</option>)}
          </select>
          {filterType && <button className="btn-link" onClick={() => setFilterType('')}>Limpar</button>}
        </div>
      </div>

      {loading && <p>Carregando evidências...</p>}
      {error && <p style={{ color: 'red' }}>Erro: {error}</p>}

      <div className="case-list-grid">
        {evidences
          .filter(ev => !filterType || (ev.evidenceType === filterType))
          .map(ev => (
          <div key={ev.evidenceId} className="case-card">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h3 style={{ margin: 0 }}>Evidência {ev.evidenceId}</h3>
            </div>
            <div className="case-meta">Tipo: {ev.evidenceType || '—'}</div>
            <div className="case-meta">Descrição: {ev.evidenceDescription || '—'}</div>
            <div className="case-meta">
              Criado por: {ev.uploadedBy !== undefined ? (uploaderNames[String(ev.uploadedBy)] ?? ev.uploadedBy) : '—'}
            </div>
            <div className="case-meta">Criado em: {ev.uploadedAt ?? '—'}</div>
            <div className="case-footer">
              <div />
              <div>
                <button className="btn-link" onClick={() => navigate(`/cases/${caseId}/evidences/${ev.evidenceId}/edit`)}>Editar</button>
                <button style={{ marginLeft: 8 }} className="btn-link evidence-delete-btn" onClick={() => handleDelete(ev.evidenceId)}>Apagar</button>
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="evidence-bottom-actions">
        <button onClick={handleAnalyze} disabled={!caseId} className="btn-link evidence-analyze-btn">
          Analisar
        </button>
      </div>

    </div>
  );
}
