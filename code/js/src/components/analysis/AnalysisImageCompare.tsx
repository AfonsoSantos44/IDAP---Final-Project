import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import '../../styles/AnalysisImageCompare.css';
import { useAuth } from '../../context/AuthContext';
import { evidenceService } from '../../services/evidenceService';
import type {
  EvidenceOutput,
  ImageEvidenceOutput,
} from '../../services/evidenceService';
import { vehicleService, type DamageOutput, type VehicleOutput } from '../../services/vehicleService';
import accidentAnalysisService, {
  type DamageSelectionRequest,
  type MeasurementOutput,
  type RulerReferencePointRequest,
} from '../../services/analysisService';

type SelectableImage = {
  evidence: EvidenceOutput;
  image: ImageEvidenceOutput;
};

type ReferencePoint = RulerReferencePointRequest & {
  valueText: string;
  markerLeft: number;
  markerTop: number;
};

function imageLabel(item: SelectableImage) {
  const id = item.evidence.evidenceId ?? item.image.evidenceId;
  return `${item.evidence.evidenceType || 'Imagem'} #${id ?? '-'}`;
}

// Área do dano marcada pelo utilizador (arrastando uma caixa sobre a imagem).
// Guarda as coordenadas na imagem (para a medição) e a caixa em % (para o overlay).
type DamageArea = {
  x1: number;
  y1: number;
  x2: number;
  y2: number;
  left: number;
  top: number;
  width: number;
  height: number;
};

// Converte a área marcada numa seleção válida para o motor: garante ordem e uma
// dimensão mínima (para que um simples clique também produza uma caixa medível).
function selectionFromDamageArea(
  area: DamageArea,
  image: ImageEvidenceOutput
): DamageSelectionRequest | null {
  if (!image.width || !image.height || image.width < 2 || image.height < 2) return null;
  const minSize = 10;
  let x1 = Math.max(0, Math.min(image.width - 2, Math.round(Math.min(area.x1, area.x2))));
  let y1 = Math.max(0, Math.min(image.height - 2, Math.round(Math.min(area.y1, area.y2))));
  let x2 = Math.min(image.width - 1, Math.round(Math.max(area.x1, area.x2)));
  let y2 = Math.min(image.height - 1, Math.round(Math.max(area.y1, area.y2)));
  if (x2 - x1 < minSize) {
    const cx = Math.round((x1 + x2) / 2);
    x1 = Math.max(0, cx - minSize);
    x2 = Math.min(image.width - 1, cx + minSize);
  }
  if (y2 - y1 < minSize) {
    const cy = Math.round((y1 + y2) / 2);
    y1 = Math.max(0, cy - minSize);
    y2 = Math.min(image.height - 1, cy + minSize);
  }
  return { x1, y1, x2, y2 };
}

function calibrationFromPoints(points: ReferencePoint[]) {
  return {
    referencePoints: points.map((point) => ({
      x: point.x,
      y: point.y,
      valueCm: Number(point.valueText),
    })),
  };
}

function hasTwoValidReferencePoints(points: ReferencePoint[]) {
  return (
    points.length === 2 &&
    points.every((point) => Number.isFinite(Number(point.valueText)))
  );
}

export default function AnalysisImageCompare() {
  const navigate = useNavigate();
  const { caseId } = useParams();

  const [images, setImages] = useState<SelectableImage[]>([]);
  const [vehicles, setVehicles] = useState<VehicleOutput[]>([]);
  const [vehicleDamages, setVehicleDamages] = useState<Record<number, DamageOutput[]>>({});
  const [firstImageId, setFirstImageId] = useState('');
  const [secondImageId, setSecondImageId] = useState('');
  const [analysisId, setAnalysisId] = useState<number | null>(null);
  const [firstMeasurement, setFirstMeasurement] = useState<MeasurementOutput | null>(null);
  const [secondMeasurement, setSecondMeasurement] = useState<MeasurementOutput | null>(null);
  const [firstReferencePoints, setFirstReferencePoints] = useState<ReferencePoint[]>([]);
  const [secondReferencePoints, setSecondReferencePoints] = useState<ReferencePoint[]>([]);
  const [firstDamageArea, setFirstDamageArea] = useState<DamageArea | null>(null);
  const [secondDamageArea, setSecondDamageArea] = useState<DamageArea | null>(null);
  const [manualCalibration, setManualCalibration] = useState(false);
  // No modo manual, qual dos dois se está a marcar na imagem: a área do dano ou os
  // pontos de referência na régua. No modo automático marca-se sempre o dano.
  const [manualTarget, setManualTarget] = useState<'damage' | 'ruler'>('damage');
  const [measurementStatus, setMeasurementStatus] = useState('');
  const [measurementStatusType, setMeasurementStatusType] =
    useState<'running' | 'success' | 'error' | ''>('');
  const [measuring, setMeasuring] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const measurementRunRef = useRef(0);
  const { logout } = useAuth();

  const goBack = () => {
    if (caseId && analysisId) {
      navigate(`/cases/${caseId}/analysis/${analysisId}`);
    } else if (caseId) {
      navigate(`/cases/${caseId}`);
    } else {
      navigate(-1);
    }
  };

  const handleLogout = async () => {
    try {
      await logout();
      navigate('/initial');
    } catch (err) {
      console.error('Logout failed', err);
    }
  };

  useEffect(() => {
    const fetchImages = async () => {
      try {
        setLoading(true);
        setError(null);

        if (!caseId) {
          throw new Error('ID do caso invalido.');
        }

        const caseNumber = Number(caseId);
        const [evidences, caseVehicles, analyses] = await Promise.all([
          evidenceService.listCaseEvidence(caseNumber),
          vehicleService.getCaseVehicles(caseNumber),
          accidentAnalysisService.listCaseAnalyses(caseNumber),
        ]);
        const imageResults = await Promise.allSettled(
          evidences
            .filter((evidence) => evidence.evidenceId !== undefined)
            .map(async (evidence) => ({
              evidence,
              image: await evidenceService.getEvidenceImage(evidence.evidenceId!),
            }))
        );

        const availableImages = imageResults
          .filter(
            (result): result is PromiseFulfilledResult<SelectableImage> =>
              result.status === 'fulfilled' && Boolean(result.value.image.filePath)
          )
          .map((result) => result.value);

        setImages(availableImages);
        setVehicles(caseVehicles);
        setAnalysisId(
          analyses[0]?.analysisId ??
            (await accidentAnalysisService.createCaseAnalysis(caseNumber)).analysisId ??
            null
        );

        const firstVehicleId = caseVehicles[0]?.vehicleId;
        const secondVehicleId = caseVehicles[1]?.vehicleId;
        const firstVehicleImage = availableImages.find(
          (item) => item.image.vehicleId === firstVehicleId
        );
        const secondVehicleImage = availableImages.find(
          (item) => item.image.vehicleId === secondVehicleId
        );

        setFirstImageId(String(firstVehicleImage?.evidence.evidenceId ?? ''));
        setSecondImageId(String(secondVehicleImage?.evidence.evidenceId ?? ''));

        const damageResults = await Promise.allSettled(
          caseVehicles
            .filter((vehicle) => vehicle.vehicleId !== undefined)
            .map(async (vehicle) => ({
              vehicleId: vehicle.vehicleId!,
              damages: await vehicleService.getVehicleDamages(vehicle.vehicleId!),
            }))
        );

        const damagesByVehicle = damageResults.reduce<Record<number, DamageOutput[]>>(
          (acc, result) => {
            if (result.status === 'fulfilled') {
              acc[result.value.vehicleId] = result.value.damages;
            }
            return acc;
          },
          {}
        );

        setVehicleDamages(damagesByVehicle);
      } catch (err: any) {
        setError(err?.message || 'Erro ao carregar imagens.');
      } finally {
        setLoading(false);
      }
    };

    fetchImages();
  }, [caseId]);

  const firstImage = useMemo(
    () =>
      images.find(
        (item) => String(item.evidence.evidenceId) === firstImageId
      ),
    [firstImageId, images]
  );

  const secondImage = useMemo(
    () =>
      images.find(
        (item) => String(item.evidence.evidenceId) === secondImageId
      ),
    [secondImageId, images]
  );

  const sameImageSelected =
    Boolean(firstImageId) && firstImageId === secondImageId;

  const firstVehicle = vehicles[0];
  const secondVehicle = vehicles[1];

  const firstVehicleImages = useMemo(
    () => images.filter((item) => item.image.vehicleId === firstVehicle?.vehicleId),
    [firstVehicle?.vehicleId, images]
  );

  const secondVehicleImages = useMemo(
    () => images.filter((item) => item.image.vehicleId === secondVehicle?.vehicleId),
    [secondVehicle?.vehicleId, images]
  );

  const firstVehicleDamages = useMemo(
    () =>
      firstVehicle?.vehicleId !== undefined
        ? vehicleDamages[firstVehicle.vehicleId] ?? []
        : [],
    [firstVehicle?.vehicleId, vehicleDamages]
  );

  const secondVehicleDamages = useMemo(
    () =>
      secondVehicle?.vehicleId !== undefined
        ? vehicleDamages[secondVehicle.vehicleId] ?? []
        : [],
    [secondVehicle?.vehicleId, vehicleDamages]
  );

  useEffect(() => {
    if (
      firstVehicleImages.length > 0 &&
      !firstVehicleImages.some((item) => String(item.evidence.evidenceId) === firstImageId)
    ) {
      setFirstImageId(String(firstVehicleImages[0].evidence.evidenceId ?? ''));
    }
  }, [firstImageId, firstVehicleImages]);

  useEffect(() => {
    if (
      secondVehicleImages.length > 0 &&
      !secondVehicleImages.some((item) => String(item.evidence.evidenceId) === secondImageId)
    ) {
      setSecondImageId(String(secondVehicleImages[0].evidence.evidenceId ?? ''));
    }
  }, [secondImageId, secondVehicleImages]);

  useEffect(() => {
    setFirstReferencePoints([]);
    setFirstDamageArea(null);
    setFirstMeasurement(null);
  }, [firstImageId]);

  useEffect(() => {
    setSecondReferencePoints([]);
    setSecondDamageArea(null);
    setSecondMeasurement(null);
  }, [secondImageId]);

  const runMeasurements = async () => {
    if (!analysisId || !firstImage || !secondImage) {
      setMeasurementStatus('Selecione duas imagens para correr a medição.');
      setMeasurementStatusType('error');
      return;
    }

    const firstEvidenceId = firstImage.evidence.evidenceId;
    const secondEvidenceId = secondImage.evidence.evidenceId;
    const firstDamageId = firstVehicleDamages[0]?.damageId;
    const secondDamageId = secondVehicleDamages[0]?.damageId;

    if (!firstEvidenceId || !secondEvidenceId) {
      setMeasurementStatus('Não foi possível identificar as evidências das imagens.');
      setMeasurementStatusType('error');
      return;
    }

    if (!firstDamageId || !secondDamageId) {
      setMeasurementStatus('Registe pelo menos um dano em cada veículo antes de correr a medição.');
      setMeasurementStatusType('error');
      return;
    }

    // É necessário indicar a ÁREA do dano (arrastar uma caixa em cada imagem). A
    // escala é calibrada automaticamente por OCR; os pontos de referência só são
    // usados no modo de calibração manual, como fallback.
    if (!firstDamageArea || !secondDamageArea) {
      setMeasurementStatus('Marque a área do dano (arraste uma caixa) em cada imagem antes de correr a medição.');
      setMeasurementStatusType('error');
      return;
    }

    const primarySelection = selectionFromDamageArea(firstDamageArea, firstImage.image);
    const comparisonSelection = selectionFromDamageArea(secondDamageArea, secondImage.image);
    if (!primarySelection || !comparisonSelection) {
      setMeasurementStatus('Não foi possível obter as dimensões das imagens para a medição.');
      setMeasurementStatusType('error');
      return;
    }

    const firstHasPoints = manualCalibration && hasTwoValidReferencePoints(firstReferencePoints);
    const secondHasPoints = manualCalibration && hasTwoValidReferencePoints(secondReferencePoints);
    const automatic = !firstHasPoints || !secondHasPoints;

    const runId = measurementRunRef.current + 1;
    measurementRunRef.current = runId;
    setMeasuring(true);
    setMeasurementStatus(
      automatic ? 'A correr medições (calibração automática)...' : 'A correr medições...'
    );
    setMeasurementStatusType('running');

    try {
      const firstCalibration = firstHasPoints
        ? calibrationFromPoints(firstReferencePoints)
        : undefined;
      const secondCalibration = secondHasPoints
        ? calibrationFromPoints(secondReferencePoints)
        : undefined;
      const [firstResult, secondResult] = await Promise.all([
        accidentAnalysisService.createMeasurement(analysisId, {
          evidenceId: firstEvidenceId,
          damageId: firstDamageId,
          comparisonEvidenceId: secondEvidenceId,
          knownTickDistanceCm: 1,
          primarySelection,
          primaryCalibration: firstCalibration,
          comparisonSelection,
          comparisonCalibration: secondCalibration,
        }),
        accidentAnalysisService.createMeasurement(analysisId, {
          evidenceId: secondEvidenceId,
          damageId: secondDamageId,
          comparisonEvidenceId: firstEvidenceId,
          knownTickDistanceCm: 1,
          primarySelection: comparisonSelection,
          primaryCalibration: secondCalibration,
          comparisonSelection: primarySelection,
          comparisonCalibration: firstCalibration,
        }),
      ]);

      if (measurementRunRef.current === runId) {
        setFirstMeasurement(firstResult);
        setSecondMeasurement(secondResult);
        setMeasurementStatus('Medições concluídas.');
        setMeasurementStatusType('success');
      }
    } catch (err: any) {
      if (measurementRunRef.current === runId) {
        setFirstMeasurement(null);
        setSecondMeasurement(null);
        const base = err?.message || 'A medição falhou.';
        setMeasurementStatus(
          automatic
            ? `${base} Não foi possível calibrar automaticamente — marque 2 pontos de referência na régua e tente novamente.`
            : base
        );
        setMeasurementStatusType('error');
      }
    } finally {
      if (measurementRunRef.current === runId) {
        setMeasuring(false);
      }
    }
  };

  return (
    <div className="homepage-wrapper analysis-image-page">
      <div className="back-container back-outside">
        <button className="back-btn" onClick={goBack}>
          Voltar
        </button>
      </div>
      <div className="logout-container">
        <button className="homepage-btn logout-btn" onClick={handleLogout}>
          Logout
        </button>
      </div>

      <div className="homepage-overlay analysis-image-overlay">
        <div className="homepage-content analysis-image-content">
          {loading && <p>A carregar imagens...</p>}

          {error && <p className="analysis-error">{error}</p>}

          {!loading && !error && (firstVehicleImages.length === 0 || secondVehicleImages.length === 0) && (
            <p className="no-evidence">
              São necessarias fotos associadas aos dois veículos do caso.
            </p>
          )}

          {!loading && !error && firstVehicleImages.length > 0 && secondVehicleImages.length > 0 && (
            <>
              <div className="image-selector-grid">
                <label className="image-select-label">
                  <span className="sr-only">Imagem esquerda</span>
                  <select
                    aria-label="Imagem esquerda"
                    value={firstImageId}
                    onChange={(event) => setFirstImageId(event.target.value)}
                  >
                    {firstVehicleImages.map((item) => (
                      <option
                        key={`first-${item.evidence.evidenceId}`}
                        value={item.evidence.evidenceId}
                      >
                        {imageLabel(item)}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="image-select-label">
                  <span className="sr-only">Imagem direita</span>
                  <select
                    aria-label="Imagem direita"
                    value={secondImageId}
                    onChange={(event) => setSecondImageId(event.target.value)}
                  >
                    {secondVehicleImages.map((item) => (
                      <option
                        key={`second-${item.evidence.evidenceId}`}
                        value={item.evidence.evidenceId}
                      >
                        {imageLabel(item)}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              {sameImageSelected && (
                <p className="analysis-warning">
                  Selecione duas imagens diferentes para comparar lado a lado.
                </p>
              )}

              {measurementStatus && (
                <p className={`analysis-measurement-status ${measurementStatusType}`}>
                  {measurementStatus}
                  {firstMeasurement?.calculatedHeightCm !== undefined &&
                  secondMeasurement?.calculatedHeightCm !== undefined
                    ? ` Alturas calculadas: ${firstMeasurement.calculatedHeightCm} cm e ${secondMeasurement.calculatedHeightCm} cm.`
                    : ''}
                </p>
              )}

              <div className="image-compare-grid">
                <div className="image-compare-column left-column">
                  <VehicleDamageCard
                    title="Veículo A"
                    vehicle={firstVehicle}
                    damages={firstVehicleDamages}
                  />
                  <ImagePreview
                    title="Imagem esquerda"
                    item={firstImage}
                    measurement={firstMeasurement}
                    measuring={measuring}
                    damageArea={firstDamageArea}
                    onDamageAreaChange={setFirstDamageArea}
                    manualCalibration={manualCalibration}
                    markTarget={manualCalibration ? manualTarget : 'damage'}
                    referencePoints={firstReferencePoints}
                    onReferencePointsChange={setFirstReferencePoints}
                  />
                </div>

                <div className="image-compare-column right-column">
                  <ImagePreview
                    title="Imagem direita"
                    item={secondImage}
                    measurement={secondMeasurement}
                    measuring={measuring}
                    damageArea={secondDamageArea}
                    onDamageAreaChange={setSecondDamageArea}
                    manualCalibration={manualCalibration}
                    markTarget={manualCalibration ? manualTarget : 'damage'}
                    referencePoints={secondReferencePoints}
                    onReferencePointsChange={setSecondReferencePoints}
                  />
                  <VehicleDamageCard
                    title="Veículo B"
                    vehicle={secondVehicle}
                    damages={secondVehicleDamages}
                  />
                </div>
              </div>

              <div className="analysis-footer-actions">
                <div className="footer-calibration-group">
                  <label className="manual-calibration-toggle">
                    <input
                      type="checkbox"
                      checked={manualCalibration}
                      onChange={(event) => setManualCalibration(event.target.checked)}
                    />
                    Calibração manual (marcar pontos de referência)
                  </label>
                  {manualCalibration && (
                    <div className="mark-target-switch" role="group" aria-label="A marcar">
                      <span>A marcar:</span>
                      <button
                        type="button"
                        className={manualTarget === 'damage' ? 'active' : ''}
                        onClick={() => setManualTarget('damage')}
                      >
                        Dano
                      </button>
                      <button
                        type="button"
                        className={manualTarget === 'ruler' ? 'active' : ''}
                        onClick={() => setManualTarget('ruler')}
                      >
                        Régua
                      </button>
                    </div>
                  )}
                </div>
                <div className="footer-action-buttons">
                  <button
                    type="button"
                    className="homepage-btn login-btn"
                    disabled={measuring || sameImageSelected}
                    onClick={runMeasurements}
                  >
                    {measuring ? 'A medir...' : 'Correr medições'}
                  </button>
                  <button
                    type="button"
                    className="homepage-btn report-btn"
                    disabled={!analysisId}
                    onClick={() =>
                      analysisId &&
                      navigate(`/cases/${caseId}/analysis/${analysisId}/report`)
                    }
                  >
                    Criar report
                  </button>
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

function ImagePreview({
  title,
  item,
  measurement,
  measuring,
  damageArea,
  onDamageAreaChange,
  manualCalibration,
  markTarget,
  referencePoints,
  onReferencePointsChange,
}: {
  title: string;
  item?: SelectableImage;
  measurement: MeasurementOutput | null;
  measuring: boolean;
  damageArea: DamageArea | null;
  onDamageAreaChange: React.Dispatch<React.SetStateAction<DamageArea | null>>;
  manualCalibration: boolean;
  markTarget: 'damage' | 'ruler';
  referencePoints: ReferencePoint[];
  onReferencePointsChange: React.Dispatch<React.SetStateAction<ReferencePoint[]>>;
}) {
  const imageRef = useRef<HTMLImageElement | null>(null);
  const evidenceId = item?.evidence.evidenceId ?? item?.image.evidenceId;
  const imageSrc =
    evidenceId !== undefined
      ? evidenceService.evidenceImageContentUrl(evidenceId)
      : '';

  const [dragStart, setDragStart] = useState<
    { natX: number; natY: number; pctX: number; pctY: number } | null
  >(null);

  const [zoom, setZoom] = useState(1);
  const clampZoom = (value: number) => Math.min(3, Math.max(1, Math.round(value * 100) / 100));
  const zoomIn = () => setZoom((current) => clampZoom(current + 0.25));
  const zoomOut = () => setZoom((current) => clampZoom(current - 0.25));
  const resetZoom = () => setZoom(1);

  // Converte a posição do rato numa posição na imagem (coordenadas naturais) e em
  // percentagem do elemento, tendo em conta o letterbox do object-fit.
  const mapEvent = (image: HTMLImageElement, clientX: number, clientY: number) => {
    if (!image.naturalWidth || !image.naturalHeight) return null;
    const bounds = image.getBoundingClientRect();
    const naturalRatio = image.naturalWidth / image.naturalHeight;
    const boundsRatio = bounds.width / bounds.height;
    const renderedWidth = boundsRatio > naturalRatio ? bounds.height * naturalRatio : bounds.width;
    const renderedHeight = boundsRatio > naturalRatio ? bounds.height : bounds.width / naturalRatio;
    const offsetX = (bounds.width - renderedWidth) / 2;
    const offsetY = (bounds.height - renderedHeight) / 2;
    const localX = Math.max(0, Math.min(renderedWidth, clientX - bounds.left - offsetX));
    const localY = Math.max(0, Math.min(renderedHeight, clientY - bounds.top - offsetY));
    return {
      natX: Math.round((localX / renderedWidth) * image.naturalWidth),
      natY: Math.round((localY / renderedHeight) * image.naturalHeight),
      pctX: ((localX + offsetX) / bounds.width) * 100,
      pctY: ((localY + offsetY) / bounds.height) * 100,
    };
  };

  type Pt = { natX: number; natY: number; pctX: number; pctY: number };
  const buildArea = (a: Pt, b: Pt): DamageArea => ({
    x1: Math.min(a.natX, b.natX),
    y1: Math.min(a.natY, b.natY),
    x2: Math.max(a.natX, b.natX),
    y2: Math.max(a.natY, b.natY),
    left: Math.min(a.pctX, b.pctX),
    top: Math.min(a.pctY, b.pctY),
    width: Math.abs(a.pctX - b.pctX),
    height: Math.abs(a.pctY - b.pctY),
  });

  const handleImageClick = (event: React.MouseEvent<HTMLImageElement>) => {
    // O clique só adiciona pontos de referência quando se está a marcar a régua.
    if (markTarget !== 'ruler') return;
    const point = mapEvent(event.currentTarget, event.clientX, event.clientY);
    if (!point) return;
    onReferencePointsChange((current) => {
      const nextPoint: ReferencePoint = {
        x: point.natX,
        y: point.natY,
        valueCm: 0,
        valueText: '',
        markerLeft: point.pctX,
        markerTop: point.pctY,
      };
      return current.length >= 2 ? [current[1], nextPoint] : [...current, nextPoint];
    });
  };

  const handleMouseDown = (event: React.MouseEvent<HTMLImageElement>) => {
    if (markTarget !== 'damage') return;
    const point = mapEvent(event.currentTarget, event.clientX, event.clientY);
    if (!point) return;
    event.preventDefault();
    setDragStart(point);
    onDamageAreaChange(buildArea(point, point));
  };

  const handleMouseMove = (event: React.MouseEvent<HTMLImageElement>) => {
    if (markTarget !== 'damage' || !dragStart) return;
    const point = mapEvent(event.currentTarget, event.clientX, event.clientY);
    if (point) onDamageAreaChange(buildArea(dragStart, point));
  };

  const handleMouseUp = (event: React.MouseEvent<HTMLImageElement>) => {
    if (markTarget !== 'damage' || !dragStart) return;
    const point = mapEvent(event.currentTarget, event.clientX, event.clientY);
    setDragStart(null);
    if (point) onDamageAreaChange(buildArea(dragStart, point));
  };

  const updateReferenceValue = (index: number, valueText: string) => {
    onReferencePointsChange((current) =>
      current.map((point, pointIndex) =>
        pointIndex === index
          ? { ...point, valueText, valueCm: Number(valueText) }
          : point
      )
    );
  };

  const clearReferencePoints = () => onReferencePointsChange([]);

  if (!item) {
    return (
      <div className="image-preview-card empty">
        <span>Nenhuma imagem selecionada.</span>
      </div>
    );
  }

  return (
    <div className="image-preview-card">
      <MeasurementSummary measurement={measurement} measuring={measuring} />

      <div className="image-zoom-controls">
        {zoom !== 1 && (
          <button type="button" className="zoom-reset" onClick={resetZoom}>
            Repor
          </button>
        )}
        <button type="button" onClick={zoomOut} disabled={zoom <= 1} aria-label="Reduzir zoom">
          −
        </button>
        <span>{Math.round(zoom * 100)}%</span>
        <button type="button" onClick={zoomIn} disabled={zoom >= 3} aria-label="Aumentar zoom">
          +
        </button>
      </div>

      <div className="image-reference-stage">
        <div
          className="image-frame"
          style={{ width: `${zoom * 100}%` }}
        >
          <img
            key={imageSrc}
            ref={imageRef}
            src={imageSrc}
            alt={item.evidence.evidenceDescription || imageLabel(item)}
            onClick={handleImageClick}
            onMouseDown={handleMouseDown}
            onMouseMove={handleMouseMove}
            onMouseUp={handleMouseUp}
            onMouseLeave={handleMouseUp}
            draggable={false}
          />
          {damageArea && (
            <div
              className="damage-area"
              style={{
                left: `${damageArea.left}%`,
                top: `${damageArea.top}%`,
                width: `${damageArea.width}%`,
                height: `${damageArea.height}%`,
              }}
            />
          )}
          {manualCalibration &&
            referencePoints.map((point, index) => (
              <span
                key={`${point.x}-${point.y}-${index}`}
                className="reference-marker"
                style={{ left: `${point.markerLeft}%`, top: `${point.markerTop}%` }}
              >
                {index + 1}
              </span>
            ))}
        </div>
      </div>

      <p className="reference-controls-hint">
        {markTarget === 'ruler'
          ? 'A marcar a régua: clique em 2 marcas da fita e escreva o valor em cm de cada.'
          : damageArea
            ? `Área do dano marcada (${damageArea.x2 - damageArea.x1}×${damageArea.y2 - damageArea.y1} px). Arraste novamente para ajustar.`
            : 'Arraste uma caixa sobre o dano na imagem.'}
      </p>

      {manualCalibration && (
      <div className="reference-controls">
        <div className="reference-controls-header">
          <span>Pontos de referência</span>
          <button type="button" onClick={clearReferencePoints}>
            Limpar
          </button>
        </div>
        <p className="reference-controls-hint">
          Calibração manual: clique em 2 marcas da régua e escreva o valor em cm de cada.
        </p>
        {[0, 1].map((index) => {
          const point = referencePoints[index];
          return (
            <label key={index} className="reference-point-row">
              <span>
                P{index + 1}: {point ? `${point.x}, ${point.y}` : 'clique na régua'}
              </span>
              <input
                type="number"
                step="0.1"
                placeholder="cm"
                value={point?.valueText ?? ''}
                disabled={!point}
                onChange={(event) => updateReferenceValue(index, event.target.value)}
              />
            </label>
          );
        })}
      </div>
      )}
    </div>
  );
}

function MeasurementSummary({
  measurement,
  measuring,
}: {
  measurement: MeasurementOutput | null;
  measuring: boolean;
}) {
  return (
    <div className="image-measurements-panel">
      <div>
        <span>Altura calculada</span>
        <strong>
          {measurement?.calculatedHeightCm !== undefined
            ? `${measurement.calculatedHeightCm} cm`
            : measuring
              ? 'A medir...'
              : '-'}
        </strong>
      </div>
      <div>
        <span>Intervalo</span>
        <strong>
          {measurement?.damageMinHeightCm !== undefined && measurement?.damageMaxHeightCm !== undefined
            ? `${measurement.damageMinHeightCm} - ${measurement.damageMaxHeightCm} cm`
            : '-'}
        </strong>
      </div>
      <div>
        <span>Confiança</span>
        <strong>
          {measurement?.confidence !== undefined
            ? `${Math.round(measurement.confidence * 100)}%`
            : '-'}
        </strong>
      </div>
    </div>
  );
}

function vehicleLabel(vehicle?: VehicleOutput) {
  if (!vehicle) return 'Sem veí­culo associado';
  const brandModel = [vehicle.brand, vehicle.model].filter(Boolean).join(' ');
  return brandModel || `Veí­culo #${vehicle.vehicleId ?? '-'}`;
}

function VehicleDamageCard({
  title,
  vehicle,
  damages,
}: {
  title: string;
  vehicle?: VehicleOutput;
  damages: DamageOutput[];
}) {
  return (
    <section className="vehicle-damage-info-card">
      <div className="vehicle-damage-info-header">
        <h2>{title}</h2>
        <span>{vehicleLabel(vehicle)}</span>
      </div>

      {vehicle ? (
        <>
          <div className="vehicle-damage-info-meta">
            <span>{vehicle.licensePlate || 'Sem matrícula'}</span>
            <span>{vehicle.yearOfFabrication ?? 'Ano desconhecido'}</span>
          </div>

          <h3 className="analysis-damage-title">Danos:</h3>

          {damages.length > 0 ? (
            <div className="analysis-damage-list">
              {damages.map((damage) => (
                <div
                  className="analysis-damage-item"
                  key={damage.damageId ?? `${damage.contactZone}-${damage.direction}`}
                >
                  <div>
                    <strong>{damage.contactZone || 'Zona sem nome'}</strong>
                    <span>{damage.deformationType || 'Tipo não definido'}</span>
                  </div>
                  <p>{damage.damageDescription || 'Sem descrição.'}</p>
                  <small>
                    Direção: {damage.direction || '-'}
                    {damage.heightCm !== null && damage.heightCm !== undefined
                      ? ` | Altura: ${damage.heightCm} cm`
                      : ''}
                  </small>
                </div>
              ))}
            </div>
          ) : (
            <p className="analysis-damage-empty">Sem danos registados.</p>
          )}
        </>
      ) : (
        <p className="analysis-damage-empty">Não existe veículo nesta posição.</p>
      )}
    </section>
  );
}
