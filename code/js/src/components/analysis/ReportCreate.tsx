import { type FormEvent, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import '../../styles/ReportCreate.css';
import accidentAnalysisService from '../../services/analysisService';
import evidenceService, {
  type ImageEvidenceOutput,
} from '../../services/evidenceService';
import vehicleService, {
  type DamageOutput,
  type VehicleOutput,
} from '../../services/vehicleService';
import { damageLabel, formatDate, groupImagesByVehicle, imageName, vehicleLabel } from './reportUtils';
import { useAuth } from '../../context/AuthContext';

export default function ReportCreate() {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const { caseId, analysisId } = useParams();
  const numericAnalysisId = useMemo(
    () => (analysisId ? Number(analysisId) : NaN),
    [analysisId]
  );
  const numericCaseId = useMemo(
    () => (caseId ? Number(caseId) : NaN),
    [caseId]
  );

  const [caseImages, setCaseImages] = useState<ImageEvidenceOutput[]>([]);
  const [caseVehicles, setCaseVehicles] = useState<VehicleOutput[]>([]);
  const [vehicleDamages, setVehicleDamages] = useState<Record<number, DamageOutput[]>>({});
  const [selectedImageIds, setSelectedImageIds] = useState<Set<number>>(
    new Set()
  );
  const [conclusion, setConclusion] = useState('');
  const [description, setDescription] = useState('');
  const [loadingImages, setLoadingImages] = useState(true);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [nextReportId, setNextReportId] = useState<number | null>(null);
  const [now, setNow] = useState(() => new Date());

  const goBack = () =>
    navigate(caseId ? `/cases/${caseId}/analysis/image` : '/cases');

  const handleLogout = async () => {
    await logout();
    navigate('/initial');
  };

  const loadCaseData = async () => {
    if (!Number.isFinite(numericCaseId)) {
      setError('ID do caso inválido.');
      setLoadingImages(false);
      return;
    }

    try {
      setLoadingImages(true);
      setError(null);
      const [evidences, vehicles, existingReports] = await Promise.all([
        evidenceService.listCaseEvidence(numericCaseId),
        vehicleService.getCaseVehicles(numericCaseId),
        Number.isFinite(numericAnalysisId)
          ? accidentAnalysisService.listAnalysisReports(numericAnalysisId)
          : Promise.resolve([]),
      ]);
      const results = await Promise.allSettled(
        evidences
          .filter((ev) => ev.evidenceId !== undefined)
          .map((ev) => evidenceService.getEvidenceImage(ev.evidenceId as number))
      );
      const images = results
        .filter(
          (result): result is PromiseFulfilledResult<ImageEvidenceOutput> =>
            result.status === 'fulfilled'
        )
        .map((result) => result.value)
        .filter((image) => image.imageEvidenceId !== undefined);
      setCaseImages(images);
      setCaseVehicles(vehicles);

      const damageEntries = await Promise.all(
        vehicles
          .filter((vehicle) => vehicle.vehicleId !== undefined)
          .map(async (vehicle) => {
            const damages = await vehicleService
              .getVehicleDamages(vehicle.vehicleId as number)
              .catch(() => []);
            return [vehicle.vehicleId as number, damages] as const;
          })
      );
      setVehicleDamages(Object.fromEntries(damageEntries));

      const lastReportId = existingReports.reduce(
        (max, report) => (report.reportId ? Math.max(max, report.reportId) : max),
        0
      );
      setNextReportId(lastReportId + 1);
    } catch (err: any) {
      setError(err?.message || 'Erro ao carregar dados do caso.');
    } finally {
      setLoadingImages(false);
    }
  };

  useEffect(() => {
    loadCaseData();
  }, [numericCaseId, numericAnalysisId]);

  useEffect(() => {
    const intervalId = window.setInterval(() => setNow(new Date()), 1000);
    return () => window.clearInterval(intervalId);
  }, []);

  const toggleImage = (imageEvidenceId: number) => {
    setSelectedImageIds((current) => {
      const next = new Set(current);
      if (next.has(imageEvidenceId)) {
        next.delete(imageEvidenceId);
      } else {
        next.add(imageEvidenceId);
      }
      return next;
    });
  };

  const createReport = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!Number.isFinite(numericAnalysisId)) {
      setError('ID da análise inválido.');
      return;
    }

    try {
      setCreating(true);
      setError(null);

      const trimmedConclusion = conclusion.trim();
      const trimmedDescription = description.trim();
      await accidentAnalysisService.createAnalysisReport(numericAnalysisId, {
        imageEvidenceIds: Array.from(selectedImageIds),
        ...(trimmedConclusion ? { conclusion: trimmedConclusion } : {}),
        ...(trimmedDescription ? { description: trimmedDescription } : {}),
      });

      navigate('/reports');
    } catch (err: any) {
      setError(err?.message || 'Erro ao criar report.');
      setCreating(false);
    }
  };

  const pickerColumns = groupImagesByVehicle(caseImages, caseVehicles);

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

          <p className="homepage-subtitle">
            Report da análise {analysisId}
          </p>

          {error && <p className="report-message error">{error}</p>}

          <form className="report-form report-form-stacked" onSubmit={createReport}>
            <div className="report-detail-block">
              <strong>Campos automáticos</strong>
              <p>Nº de relatório: {nextReportId ?? '-'}</p>
              <p>Nº do processo: {Number.isFinite(numericCaseId) ? numericCaseId : '-'}</p>
              <p>Criado em: {formatDate(now)}</p>
            </div>

            <label>
              Conclusão da análise
              <textarea
                value={conclusion}
                onChange={(event) => setConclusion(event.target.value)}
                placeholder="Insira a conclusão da análise"
                rows={4}
                disabled={creating}
              />
            </label>

            <label>
              Descrição
              <textarea
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                placeholder="Insira a descrição do relatório"
                rows={4}
                disabled={creating}
              />
            </label>

            <div className="report-detail-block">
              <strong>Imagens</strong>
              {loadingImages ? (
                <p>A carregar imagens do caso...</p>
              ) : pickerColumns.length > 0 ? (
                <div className="report-image-columns">
                  {pickerColumns.map((column) => (
                    <div className="report-image-column" key={column.vehicleId}>
                      <h4>{vehicleLabel(column.vehicle)}</h4>
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
                            <li key={image.imageEvidenceId}>
                              <label>
                                <input
                                  type="checkbox"
                                  checked={
                                    image.imageEvidenceId !== undefined &&
                                    selectedImageIds.has(image.imageEvidenceId)
                                  }
                                  disabled={creating}
                                  onChange={() =>
                                    image.imageEvidenceId !== undefined &&
                                    toggleImage(image.imageEvidenceId)
                                  }
                                />
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
                              </label>
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
                <p>Sem imagens associadas a este caso.</p>
              )}
            </div>

            <button
              className="homepage-btn login-btn"
              type="submit"
              disabled={creating || loadingImages}
            >
              {creating ? 'A criar...' : 'Criar report'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
