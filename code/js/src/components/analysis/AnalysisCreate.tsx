import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import '../../styles/AnalysisCreate.css';
import type { CaseDetailsOutput } from '../../types/caseTypes';
import { evidenceService, EvidenceOutput } from '../../services/evidenceService';

import { caseService } from '../../services/caseService';

function evidenceKind(type?: string) {
  const normalized = String(type ?? '').trim().toLowerCase();

  if (normalized === 'foto' || normalized === 'imagem' || normalized === 'image') return 'photo';
  if (normalized === 'documento' || normalized === 'document' || normalized === 'pdf') return 'document';
  if (normalized === 'medida' || normalized === 'measurement') return 'measurement';
  return 'other';
}

function evidenceIcon(type?: string) {
  const kind = evidenceKind(type);

  if (kind === 'document') return 'ðŸ“„';
  if (kind === 'measurement') return 'ðŸ“';
  return 'ðŸ§¾';
}

function EvidenceVisual({ evidence }: { evidence: EvidenceOutput }) {
  const [imageFailed, setImageFailed] = useState(false);
  const isPhoto = evidenceKind(evidence.evidenceType) === 'photo';
  const evidenceId = evidence.evidenceId;

  if (isPhoto && evidenceId !== undefined && !imageFailed) {
    return (
      <div className="evidence-preview">
        <img
          src={evidenceService.evidenceImageContentUrl(evidenceId)}
          alt={evidence.evidenceDescription || 'Preview da foto'}
          onError={() => setImageFailed(true)}
        />
      </div>
    );
  }

  return (
    <div className={`evidence-icon ${evidenceKind(evidence.evidenceType)}`}>
      {isPhoto ? 'ðŸ–¼ï¸' : evidenceIcon(evidence.evidenceType)}
    </div>
  );
}

export default function AnalysisCreate() {
  const navigate = useNavigate();
  const { caseId } = useParams();

  const [caseData, setCaseData] = useState<CaseDetailsOutput | null>(null);
  const [evidences, setEvidences] = useState<EvidenceOutput[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const goBack = () =>
    navigate(-1);

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

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        setError(null);

        if (!caseId) {
          throw new Error('ID do caso invÃ¡lido.');
        }

        const id = Number(caseId);

        const [caseRes, evidenceRes] = await Promise.all([
          caseService.getCase(id),
          evidenceService.listCaseEvidence(id),
        ]);

        setCaseData(caseRes);
        setEvidences(evidenceRes);
      } catch (err: any) {
        setError(err?.message || 'Erro ao carregar dados.');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [caseId]);

  return (
    <div className="homepage-wrapper">
      <div className="back-container back-outside">
        <button className="back-btn" onClick={goBack}>
          Voltar
        </button>
      </div>

      <div className="homepage-overlay analysis-create-overlay">
        <div className="homepage-content analysis-create-content">
          <h1 className="homepage-title">IDAP</h1>

          <p className="homepage-subtitle">
            Informações do caso {caseId}
          </p>

          {loading && <p>A carregar dados...</p>}

          {error && <p style={{ color: 'red' }}>{error}</p>}

          {/* CASE INFO */}
          {caseData && (
            <div className="case-grid">
              <div className="info-card">
                <span className="label">Estado: </span>
                <span className={`value status ${getStatusClass(caseData.status)}`}>
                  {getStatusLabel(caseData.status)}
                </span>
              </div>

              <div className="info-card wide">
                <span className="label">Descrição: </span>
                <span className="value">
                  {caseData.description}
                </span>
              </div>

              <div className="info-card">
                <span className="label">Criado em </span>
                <span className="value">
                  {new Date(caseData.createdAt).toLocaleDateString()}
                </span>
              </div>
            </div>
          )}

          {/* EVIDENCES */}
          {caseData && (
            <div className="evidence-section">
              <h2 className="section-title">Evidências</h2>

              {evidences.length > 0 ? (
                <div className="evidence-grid">
                  {evidences.map((ev) => (
                    <div
                      key={ev.evidenceId}
                      className="evidence-card"
                    >
                      <EvidenceVisual evidence={ev} />

                      <div>
                        <div className="evidence-name">
                          {ev.evidenceType ||
                            'Tipo desconhecido'}
                        </div>

                        <div className="evidence-type">
                          {ev.evidenceDescription ||
                            'Sem descriÃ§Ã£o'}
                        </div>

                        <div className="evidence-meta">
                          {ev.uploadedAt
                            ? new Date(
                                ev.uploadedAt
                              ).toLocaleString()
                            : ''}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="no-evidence">
                  Sem evidências associadas ao caso.
                </p>
              )}
            </div>
          )}

          {/* BUTTON */}
          {caseData && (
            <div className="homepage-buttons">
              <button
                className="homepage-btn login-btn"
                onClick={() =>
                  navigate(
                    `/cases/${caseId}/analysis/image`
                  )
                }
              >
                Analisar Imagens
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
