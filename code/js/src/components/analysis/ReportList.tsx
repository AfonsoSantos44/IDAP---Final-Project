import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import '../../styles/ReportCreate.css';
import accidentAnalysisService, { type ReportOutput } from '../../services/analysisService';
import { useAuth } from '../../context/AuthContext';

export default function ReportList() {
  const navigate = useNavigate();
  const { logout } = useAuth();

  const [reports, setReports] = useState<ReportOutput[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const goBack = () => navigate('/home');

  const handleLogout = async () => {
    await logout();
    navigate('/initial');
  };

  const goToDetails = (reportId?: number) => {
    if (!reportId) return;
    navigate(`/reports/${reportId}`);
  };

  const loadReports = async () => {
    try {
      setLoading(true);
      setError(null);
      const loadedReports = await accidentAnalysisService.listReports();
      setReports(loadedReports);
    } catch (err: any) {
      setError(err?.message || 'Erro ao carregar reports.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadReports();
  }, []);

  return (
    <div className="homepage-wrapper report-create-page">
      <div className="logout-wrapper">
        <button className="logout-btn" onClick={handleLogout}>Logout</button>
      </div>
      <div className="back-container back-outside">
        <button className="back-btn" onClick={goBack}>
          Voltar
        </button>
      </div>

      <div className="homepage-overlay report-create-overlay">
        <div className="homepage-content report-create-content">
          <h1 className="homepage-title">IDAP</h1>

          <p className="homepage-subtitle">Reports</p>

          {error && <p className="report-message error">{error}</p>}

          <section className="report-history" style={{ marginTop: '1.6rem' }}>
            {loading ? (
              <p className="report-empty">A carregar reports...</p>
            ) : reports.length > 0 ? (
              <div className="report-list">
                {reports.map((report) => (
                  <article className="report-card" key={report.reportId}>
                    <div className="report-card-info">
                      <div>
                        <strong>Relatório {report.reportId ?? '-'}</strong>
                      </div>

                      <p>Nº do Caso: {report.caseId ?? '-'}</p>

                      <div className="report-detail-block">
                        <strong>Conclusão da análise</strong>
                        <p>{report.conclusion || 'Sem conclusão inserida.'}</p>
                      </div>
                    </div>

                    <button
                      className="homepage-btn login-btn report-card-action"
                      onClick={() => goToDetails(report.reportId)}
                    >
                      Ver Mais
                    </button>
                  </article>
                ))}
              </div>
            ) : (
              <p className="report-empty">Ainda não existem reports.</p>
            )}
          </section>
        </div>
      </div>
    </div>
  );
}
