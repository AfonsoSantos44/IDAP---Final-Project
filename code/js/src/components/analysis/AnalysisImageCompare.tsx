import React, { useEffect, useMemo, useState } from 'react';
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

function imageSource(filePath?: string) {
  if (!filePath) return '';
  if (/^(https?:|data:|blob:|\/)/.test(filePath)) return filePath;
  return `/${filePath}`;
}

function imageLabel(item: SelectableImage) {
  const id = item.evidence.evidenceId ?? item.image.evidenceId;
  return `${item.evidence.evidenceType || 'Imagem'} #${id ?? '-'}`;
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
        src={imageSource(item.image.filePath)}
        alt={item.evidence.evidenceDescription || imageLabel(item)}
      />

      <p>{item.evidence.evidenceDescription || 'Sem descricao.'}</p>
    </div>
  );
}
