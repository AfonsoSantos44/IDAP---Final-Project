import React, { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import '../../styles/EvidenceCreate.css';
import accidentAnalysisService from '../../services/accidentAnalysisService';

export default function AnalysisCreate() {
  const navigate = useNavigate();
  const { caseId } = useParams();

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const goBack = () =>
    navigate(caseId ? `/cases/${caseId}/menu` : '/cases');

  const handleCreate = async () => {
    try {
      setError(null);
      setSubmitting(true);

      if (!caseId) {
        throw new Error('ID do caso inválido.');
      }

      const analysis =
        await accidentAnalysisService.createCaseAnalysis(
          Number(caseId)
        );

      navigate(
        `/cases/${caseId}/analyses/${analysis.analysisId}`
      );
    } catch (err: any) {
      setError(
        err?.message || 'Erro ao criar análise.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="homepage-wrapper">
      <div className="back-container back-outside">
        <button
          className="page-btn secondary back-btn"
          onClick={goBack}
        >
          Voltar
        </button>
      </div>

      <div className="homepage-overlay">
        <div
          className="homepage-content"
          style={{ textAlign: 'left' }}
        >
          <h1 className="homepage-title">IDAP</h1>

          <p className="homepage-subtitle">
            Criar uma nova análise para o caso {caseId}.
          </p>

          <div
            style={{
              marginTop: 32,
              padding: 24,
              background: '#f7f8ff',
              borderRadius: 16,
              border: '1px solid rgba(78,75,250,0.1)',
            }}
          >
            <h2
              style={{
                marginTop: 0,
                color: '#232b5d',
              }}
            >
              Nova Análise
            </h2>

            <p
              style={{
                color: '#5e6689',
                lineHeight: 1.6,
              }}
            >
              Será criada uma nova análise associada
              ao caso selecionado.
            </p>

            {error && (
              <div
                style={{
                  marginTop: 16,
                  color: 'red',
                }}
              >
                {error}
              </div>
            )}

            <div
              style={{
                marginTop: 24,
                display: 'flex',
                gap: 12,
              }}
            >
              <button
                className="homepage-btn login-btn"
                onClick={handleCreate}
                disabled={submitting}
              >
                {submitting
                  ? 'A criar...'
                  : 'Criar Análise'}
              </button>

              <button
                className="page-btn"
                onClick={goBack}
                disabled={submitting}
              >
                Cancelar
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}