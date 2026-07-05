import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import '../../styles/AnalysisImageCompare.css';
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

function fullImageSelection(image: ImageEvidenceOutput): DamageSelectionRequest | null {
  if (!image.width || !image.height || image.width < 2 || image.height < 2) return null;
  const width = image.width - 1;
  const height = image.height - 1;

  return {
    x1: 0,
    y1: 0,
    x2: width,
    y2: height,
  };
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
  const [measurementStatus, setMeasurementStatus] = useState('');
  const [measurementStatusType, setMeasurementStatusType] =
    useState<'running' | 'success' | 'error' | ''>('');
  const [measuring, setMeasuring] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const measurementRunRef = useRef(0);

  const goBack = () => navigate(-1);

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
    setFirstMeasurement(null);
  }, [firstImageId]);

  useEffect(() => {
    setSecondReferencePoints([]);
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

    if (!hasTwoValidReferencePoints(firstReferencePoints) || !hasTwoValidReferencePoints(secondReferencePoints)) {
      setMeasurementStatus('Selecione 2 pontos de referência em cada imagem e preencha os respetivos valores em cm.');
      setMeasurementStatusType('error');
      return;
    }

    const primarySelection = fullImageSelection(firstImage.image);
    const comparisonSelection = fullImageSelection(secondImage.image);
    if (!primarySelection || !comparisonSelection) {
      setMeasurementStatus('Não foi possível obter as dimensões das imagens para a medição.');
      setMeasurementStatusType('error');
      return;
    }

    const runId = measurementRunRef.current + 1;
    measurementRunRef.current = runId;
    setMeasuring(true);
    setMeasurementStatus('A correr medições...');
    setMeasurementStatusType('running');

    try {
      const firstCalibration = calibrationFromPoints(firstReferencePoints);
      const secondCalibration = calibrationFromPoints(secondReferencePoints);
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
        setMeasurementStatus(err?.message || 'A medição falhou.');
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

      <div className="homepage-overlay analysis-image-overlay">
        <div className="homepage-content analysis-image-content">
          <h1 className="homepage-title">IDAP</h1>

          <p className="homepage-subtitle">
            Comparar imagens do caso {caseId}
          </p>

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
                  Imagem esquerda
                  <select
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
                  Imagem direita
                  <select
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

              <div className="analysis-measurement-actions">
                <button
                  type="button"
                  className="homepage-btn login-btn"
                  disabled={measuring || sameImageSelected}
                  onClick={runMeasurements}
                >
                  {measuring ? 'A medir...' : 'Correr medições'}
                </button>
              </div>

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
  referencePoints,
  onReferencePointsChange,
}: {
  title: string;
  item?: SelectableImage;
  measurement: MeasurementOutput | null;
  measuring: boolean;
  referencePoints: ReferencePoint[];
  onReferencePointsChange: React.Dispatch<React.SetStateAction<ReferencePoint[]>>;
}) {
  const imageRef = useRef<HTMLImageElement | null>(null);
  const evidenceId = item?.evidence.evidenceId ?? item?.image.evidenceId;
  const imageSrc =
    evidenceId !== undefined
      ? evidenceService.evidenceImageContentUrl(evidenceId)
      : '';

  const handleImageClick = (event: React.MouseEvent<HTMLImageElement>) => {
    const image = event.currentTarget;
    if (!image.naturalWidth || !image.naturalHeight) return;

    const bounds = image.getBoundingClientRect();
    const naturalRatio = image.naturalWidth / image.naturalHeight;
    const boundsRatio = bounds.width / bounds.height;
    const renderedWidth = boundsRatio > naturalRatio ? bounds.height * naturalRatio : bounds.width;
    const renderedHeight = boundsRatio > naturalRatio ? bounds.height : bounds.width / naturalRatio;
    const offsetX = (bounds.width - renderedWidth) / 2;
    const offsetY = (bounds.height - renderedHeight) / 2;
    const clickX = event.clientX - bounds.left - offsetX;
    const clickY = event.clientY - bounds.top - offsetY;

    if (clickX < 0 || clickY < 0 || clickX > renderedWidth || clickY > renderedHeight) return;

    const x = Math.round((clickX / renderedWidth) * image.naturalWidth);
    const y = Math.round((clickY / renderedHeight) * image.naturalHeight);
    const markerLeft = ((event.clientX - bounds.left) / bounds.width) * 100;
    const markerTop = ((event.clientY - bounds.top) / bounds.height) * 100;

    onReferencePointsChange((current) => {
      const nextPoint: ReferencePoint = {
        x,
        y,
        valueCm: 0,
        valueText: '',
        markerLeft,
        markerTop,
      };
      return current.length >= 2 ? [current[1], nextPoint] : [...current, nextPoint];
    });
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
      <div className="image-preview-header">
        <h2>{title}</h2>
        <span>{imageLabel(item)}</span>
      </div>

      <div className="image-reference-stage">
        <img
          key={imageSrc}
          ref={imageRef}
          src={imageSrc}
          alt={item.evidence.evidenceDescription || imageLabel(item)}
          onClick={handleImageClick}
          draggable={false}
        />
        {referencePoints.map((point, index) => (
          <span
            key={`${point.x}-${point.y}-${index}`}
            className="reference-marker"
            style={{ left: `${point.markerLeft}%`, top: `${point.markerTop}%` }}
          >
            {index + 1}
          </span>
        ))}
      </div>

      <div className="reference-controls">
        <div className="reference-controls-header">
          <span>Pontos de referência</span>
          <button type="button" onClick={clearReferencePoints}>
            Limpar
          </button>
        </div>
        {[0, 1].map((index) => {
          const point = referencePoints[index];
          return (
            <label key={index} className="reference-point-row">
              <span>
                P{index + 1}: {point ? `${point.x}, ${point.y}` : 'clique na imagem'}
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

      <MeasurementSummary measurement={measurement} measuring={measuring} />

      <p>{item.evidence.evidenceDescription || 'Sem descricao.'}</p>
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
