import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import '../../styles/AnalysisImageCompare.css';
import { evidenceService } from '../../services/evidenceService';
import type {
  EvidenceOutput,
  ImageEvidenceOutput,
} from '../../services/evidenceService';

type SelectableImage = {
  evidence: EvidenceOutput;
  image: ImageEvidenceOutput;
};

type ImageDisplayMeasurements = {
  naturalWidth: number;
  naturalHeight: number;
  displayedWidth: number;
  displayedHeight: number;
  resizeFactor: number;
};

function imageLabel(item: SelectableImage) {
  const id = item.evidence.evidenceId ?? item.image.evidenceId;
  return `${item.evidence.evidenceType || 'Imagem'} #${id ?? '-'}`;
}

function getImageDisplayMeasurements(
  image: HTMLImageElement
): ImageDisplayMeasurements {
  const bounds = image.getBoundingClientRect();
  const naturalWidth = image.naturalWidth;
  const naturalHeight = image.naturalHeight;
  const displayedWidth = Math.round(bounds.width);
  const displayedHeight = Math.round(bounds.height);

  return {
    naturalWidth,
    naturalHeight,
    displayedWidth,
    displayedHeight,
    resizeFactor: naturalWidth > 0 ? displayedWidth / naturalWidth : 1,
  };
}

export default function AnalysisImageCompare() {
  const navigate = useNavigate();
  const { caseId } = useParams();

  const [images, setImages] = useState<SelectableImage[]>([]);
  const [firstImageId, setFirstImageId] = useState('');
  const [secondImageId, setSecondImageId] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const goBack = () => navigate(-1);

  useEffect(() => {
    const fetchImages = async () => {
      try {
        setLoading(true);
        setError(null);

        if (!caseId) {
          throw new Error('ID do caso invalido.');
        }

        const evidences = await evidenceService.listCaseEvidence(Number(caseId));
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
        setFirstImageId(String(availableImages[0]?.evidence.evidenceId ?? ''));
        setSecondImageId(String(availableImages[1]?.evidence.evidenceId ?? ''));
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

          {!loading && !error && images.length < 2 && (
            <p className="no-evidence">
              Sao necessarias pelo menos duas imagens associadas as evidencias do caso.
            </p>
          )}

          {!loading && !error && images.length >= 2 && (
            <>
              <div className="image-selector-grid">
                <label className="image-select-label">
                  Imagem esquerda
                  <select
                    value={firstImageId}
                    onChange={(event) => setFirstImageId(event.target.value)}
                  >
                    {images.map((item) => (
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
                    {images.map((item) => (
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

              <div className="image-compare-grid">
                <ImagePreview title="Imagem esquerda" item={firstImage} />
                <ImagePreview title="Imagem direita" item={secondImage} />
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
}: {
  title: string;
  item?: SelectableImage;
}) {
  const imageRef = useRef<HTMLImageElement | null>(null);
  const [measurements, setMeasurements] =
    useState<ImageDisplayMeasurements | null>(null);

  const evidenceId = item?.evidence.evidenceId ?? item?.image.evidenceId;
  const imageSrc =
    evidenceId !== undefined
      ? evidenceService.evidenceImageContentUrl(evidenceId)
      : '';

  const updateMeasurements = useCallback(() => {
    const image = imageRef.current;
    if (!image || !image.complete || image.naturalWidth === 0) return;

    setMeasurements(getImageDisplayMeasurements(image));
  }, []);

  useEffect(() => {
    setMeasurements(null);
    const frame = window.requestAnimationFrame(updateMeasurements);

    return () => window.cancelAnimationFrame(frame);
  }, [imageSrc, updateMeasurements]);

  useEffect(() => {
    const image = imageRef.current;
    if (!image) return;

    const resizeObserver =
      typeof ResizeObserver !== 'undefined'
        ? new ResizeObserver(updateMeasurements)
        : null;

    resizeObserver?.observe(image);
    window.addEventListener('resize', updateMeasurements);

    return () => {
      resizeObserver?.disconnect();
      window.removeEventListener('resize', updateMeasurements);
    };
  }, [imageSrc, updateMeasurements]);

  const handleImageLoad = (event: React.SyntheticEvent<HTMLImageElement>) => {
    setMeasurements(getImageDisplayMeasurements(event.currentTarget));
  };

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

      <img
        key={imageSrc}
        ref={imageRef}
        src={imageSrc}
        alt={item.evidence.evidenceDescription || imageLabel(item)}
        onLoad={handleImageLoad}
      />

      <div className="image-measurements-panel">
        <div>
          <span>Altura original</span>
          <strong>{item.image.height ?? measurements?.naturalHeight ?? '-'} px</strong>
        </div>
        <div>
          <span>Altura exibida</span>
          <strong>{measurements ? `${measurements.displayedHeight} px` : '-'}</strong>
        </div>
        <div>
          <span>Resize</span>
          <strong>
            {measurements
              ? `${Math.round(measurements.resizeFactor * 100)}%`
              : '-'}
          </strong>
        </div>
      </div>

      <p>{item.evidence.evidenceDescription || 'Sem descricao.'}</p>
    </div>
  );
}
