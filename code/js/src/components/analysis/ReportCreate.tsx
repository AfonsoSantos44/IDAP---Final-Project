import { type FormEvent, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import '../../styles/ReportCreate.css';
import accidentAnalysisService, {
  type ReportOutput,
} from '../../services/analysisService';

function formatDate(value?: string) {
  if (!value) return '-';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

function reportName(report: ReportOutput) {
  if (!report.filePath) return `Report #${report.reportId ?? '-'}`;
  const normalized = report.filePath.replaceAll('\\', '/');
  return normalized.split('/').filter(Boolean).at(-1) || report.filePath;
}

export default function ReportCreate() {
  const navigate = useNavigate();
  const { caseId, analysisId } = useParams();
  const numericAnalysisId = useMemo(
    () => (analysisId ? Number(analysisId) : NaN),
    [analysisId]
  );

  const [reports, setReports] = useState<ReportOutput[]>([]);
  const [filePath, setFilePath] = useState('');
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [status, setStatus] = useState('');

  const goBack = () =>
    navigate(caseId ? `/cases/${caseId}/analysis/image` : '/cases');

  const loadReports = async () => {
    if (!Number.isFinite(numericAnalysisId)) {
      setError('ID da análise inválido.');
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const loadedReports =
        await accidentAnalysisService.listAnalysisReports(numericAnalysisId);
      setReports(loadedReports);
    } catch (err: any) {
      setError(err?.message || 'Erro ao carregar reports.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadReports();
  }, [numericAnalysisId]);

  const createReport = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!Number.isFinite(numericAnalysisId)) {
      setError('ID da análise inválido.');
      return;
    }

    try {
      setCreating(true);
      setError(null);
      setStatus('');

      const trimmedPath = filePath.trim();
      const createdReport =
        await accidentAnalysisService.createAnalysisReport(
          numericAnalysisId,
          trimmedPath ? { filePath: trimmedPath } : {}
        );

      setReports((current) => [createdReport, ...current]);
      setFilePath('');
      setStatus('Report criado com sucesso.');
    } catch (err: any) {
      setError(err?.message || 'Erro ao criar report.');
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="homepage-wrapper report-create-page">
      <div className="back-container back-outside">
        <button className="back-btn" onClick={goBack}>
          Voltar
        </button>
      </div>

      <div className="homepage-overlay report-create-overlay">
        <div className="homepage-content report-create-content">
          <h1 className="homepage-title">IDAP</h1>

          <p className="homepage-subtitle">
            Report da análise {analysisId}
          </p>

          {error && <p className="report-message error">{error}</p>}
          {status && <p className="report-message success">{status}</p>}

          <form className="report-form" onSubmit={createReport}>
            <label>
              Caminho do ficheiro
              <input
                type="text"
                value={filePath}
                onChange={(event) => setFilePath(event.target.value)}
                placeholder="Opcional"
                disabled={creating}
              />
            </label>

            <button
              className="homepage-btn login-btn"
              type="submit"
              disabled={creating || loading}
            >
              {creating ? 'A criar...' : 'Criar report'}
            </button>
          </form>

          <section className="report-history">
            <div className="report-history-header">
              <h2>Reports criados</h2>
              <span>{reports.length}</span>
            </div>

            {loading ? (
              <p className="report-empty">A carregar reports...</p>
            ) : reports.length > 0 ? (
              <div className="report-list">
                {reports.map((report) => (
                  <article
                    className="report-card"
                    key={report.reportId ?? report.filePath}
                  >
                    <div>
                      <strong>{reportName(report)}</strong>
                      <span>Report #{report.reportId ?? '-'}</span>
                    </div>
                    <p>{report.filePath || 'Sem caminho definido.'}</p>
                    <small>{formatDate(report.generatedAt)}</small>
                  </article>
                ))}
              </div>
            ) : (
              <p className="report-empty">Ainda não existem reports nesta análise.</p>
            )}
          </section>
        </div>
      </div>
    </div>
  );
}
