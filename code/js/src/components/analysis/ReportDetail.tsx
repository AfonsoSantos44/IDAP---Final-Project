import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import '../../styles/ReportCreate.css';
import accidentAnalysisService, {
  type ReportImageOutput,
  type ReportOutput,
} from '../../services/analysisService';
import evidenceService from '../../services/evidenceService';
import vehicleService, { type DamageOutput } from '../../services/vehicleService';
import { damageLabel, formatDate, groupImagesByVehicle, imageName, vehicleLabel } from './reportUtils';
import { useAuth } from '../../context/AuthContext';

export default function ReportDetail() {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const { reportId } = useParams();
  const numericReportId = useMemo(
    () => (reportId ? Number(reportId) : NaN),
    [reportId]
  );

  const [report, setReport] = useState<ReportOutput | null>(null);
  const [vehicleDamages, setVehicleDamages] = useState<Record<number, DamageOutput[]>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const goBack = () => navigate('/reports');

  const handleLogout = async () => {
    await logout();
    navigate('/initial');
  };

  const loadReport = async () => {
    if (!Number.isFinite(numericReportId)) {
      setError('ID do report inválido.');
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const loadedReport = await accidentAnalysisService.getReportById(numericReportId);
      setReport(loadedReport);

      const vehicleIds = Array.from(
        new Set(
          (loadedReport.vehicles ?? [])
            .map((vehicle) => vehicle.vehicleId)
            .filter((id): id is number => id !== undefined)
        )
      );
      const damageEntries = await Promise.all(
        vehicleIds.map(async (vehicleId) => {
          const damages = await vehicleService
            .getVehicleDamages(vehicleId)
            .catch(() => []);
          return [vehicleId, damages] as const;
        })
      );
      setVehicleDamages(Object.fromEntries(damageEntries));
    } catch (err: any) {
      setError(err?.message || 'Erro ao carregar o report.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadReport();
  }, [numericReportId]);

  const columns = report
    ? groupImagesByVehicle<ReportImageOutput>(report.images ?? [], report.vehicles ?? [])
    : [];

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

          <p className="homepage-subtitle">Relatório {reportId}</p>

          {error && <p className="report-message error">{error}</p>}

          {loading ? (
            <p className="report-empty">A carregar report...</p>
          ) : report ? (
            <section className="report-history">
              <article className="report-card">
                <div>
                  <strong>Relatório {report.reportId ?? '-'}</strong>
                  <span>
                    Análise #{report.analysisId ?? '-'} | Nº do Caso {report.caseId ?? '-'}
                  </span>
                </div>

                <div className="report-detail-block">
                  <strong>Conclusão da análise</strong>
                  <p>{report.conclusion || 'Sem conclusão inserida.'}</p>
                </div>

                <div className="report-detail-block">
                  <strong>Descrição</strong>
                  <p>{report.description || 'Sem descrição inserida.'}</p>
                </div>

                <div className="report-detail-block">
                  <strong>Imagens associadas</strong>
                  {columns.length > 0 ? (
                    <div className="report-image-columns report-image-columns--detail">
                      {columns.map((column) => (
                        <div className="report-image-column" key={column.vehicleId}>
                          <h4>
                            {vehicleLabel(column.vehicle)}
                            {column.vehicle?.role ? ` (${column.vehicle.role})` : ''}
                          </h4>
                          <div className="report-vehicle-damages">
                            <strong>Danos</strong>
                            {vehicleDamages[column.vehicleId]?.length ? (
                              <ul>
                                {vehicleDamages[column.vehicleId].map((damage) => (
                                  <li key={damage.damageId}>
                                    {damageLabel(damage)}
                                    {damage.damageDescription
                                      ? `: ${damage.damageDescription}`
                                      : ''}
                                  </li>
                                ))}
                              </ul>
                            ) : (
                              <p>Sem danos registados.</p>
                            )}
                          </div>
                          {column.images.length > 0 ? (
                            <ul className="report-image-picker">
                              {column.images.map((image, index) => (
                                <li key={image.imageEvidenceId ?? image.filePath}>
                                  {image.evidenceId !== undefined && (
                                    <img
                                      className="report-image-thumb"
                                      src={evidenceService.evidenceImageContentUrl(
                                        image.evidenceId
                                      )}
                                      alt={imageName(image.filePath)}
                                    />
                                  )}
                                  <span>Foto#{index + 1}</span>
                                </li>
                              ))}
                            </ul>
                          ) : (
                            <p>Sem fotos.</p>
                          )}
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p>Sem imagens associadas.</p>
                  )}
                </div>

                <small>{formatDate(report.generatedAt)}</small>
              </article>
            </section>
          ) : (
            <p className="report-empty">Report não encontrado.</p>
          )}
        </div>
      </div>
    </div>
  );
}
