import { useCallback, useEffect, useRef, useState } from "react";
import { FaceLandmarker, FilesetResolver } from "@mediapipe/tasks-vision";
import { Camera, Upload, X, ScanFace } from "lucide-react";

/* =========================
   CONSTANTS
========================= */

const WASM_URL =
  "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.14/wasm";
const MODEL_URL =
  "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task";
const MAX_FILE_BYTES = 10 * 1024 * 1024;

const MSG = {
  loadFailed:
    "Unable to load face analysis. Check your internet connection and try again.",
  notImage: "Please select an image file.",
  tooLarge: "Please select an image smaller than 10 MB.",
  unreadable: "Unable to read the selected photo.",
  notReady: "Please upload a photo and wait for the analyzer to load.",
  noFace: "No face detected. Please use a clear, front-facing photo.",
  unmeasurable: "Could not measure the face clearly. Try another photo.",
  analysisFailed: "Face analysis failed. Please try another photo.",
  cameraFailed: "Camera permission denied or camera unavailable.",
};

// MediaPipe face mesh landmark indices used for measurements
const LANDMARKS = {
  faceLeft: 234,
  faceRight: 454,
  faceTop: 10,
  faceChin: 152,
  foreheadLeft: 103,
  foreheadRight: 332,
  jawLeft: 172,
  jawRight: 397,
  cheekLeft: 123,
  cheekRight: 352,
};

const FACE_SHAPES = {
  Oval: {
    description: "Your face appears balanced, with a gently rounded jawline.",
    frames: ["Rectangle", "Square", "Aviator", "Geometric"],
  },
  Round: {
    description:
      "Your face appears to have soft curves and similar width and length.",
    frames: ["Rectangle", "Square", "Geometric", "Wayfarer"],
  },
  Square: {
    description:
      "Your face appears to have a defined jawline and broader forehead.",
    frames: ["Round", "Oval", "Aviator", "Cat-eye"],
  },
  Heart: {
    description:
      "Your face appears wider around the forehead and narrower towards the chin.",
    frames: ["Oval", "Round", "Rimless", "Lightweight frames"],
  },
  Diamond: {
    description:
      "Your face appears wider around the cheekbones, with a narrower forehead and jaw.",
    frames: ["Oval", "Cat-eye", "Rimless", "Browline"],
  },
  Oblong: {
    description: "Your face appears longer than it is wide.",
    frames: ["Oversized", "Square", "Wayfarer", "Deep frames"],
  },
};

// Visually hidden but still focusable, so keyboard users can reach the file input.
const VISUALLY_HIDDEN = {
  position: "absolute",
  width: 1,
  height: 1,
  padding: 0,
  margin: -1,
  overflow: "hidden",
  clip: "rect(0, 0, 0, 0)",
  whiteSpace: "nowrap",
  border: 0,
};

/* =========================
   MODEL LOADING
   Loaded once per page and reused, instead of re-downloading on every open.
========================= */

let landmarkerPromise = null;

function getLandmarker() {
  if (!landmarkerPromise) {
    landmarkerPromise = (async () => {
      const vision = await FilesetResolver.forVisionTasks(WASM_URL);
      const options = { runningMode: "IMAGE", numFaces: 1 };

      try {
        return await FaceLandmarker.createFromOptions(vision, {
          baseOptions: { modelAssetPath: MODEL_URL, delegate: "GPU" },
          ...options,
        });
      } catch {
        // Some devices and browsers cannot use the GPU delegate; fall back to CPU.
        return await FaceLandmarker.createFromOptions(vision, {
          baseOptions: { modelAssetPath: MODEL_URL, delegate: "CPU" },
          ...options,
        });
      }
    })().catch((err) => {
      landmarkerPromise = null; // allow a retry on the next open
      throw err;
    });
  }
  return landmarkerPromise;
}

/* =========================
   MEASUREMENT
   Pure function: landmarks in, face shape name out (or null if unmeasurable).
========================= */

function classifyFaceShape(points) {
  const dist = (a, b) =>
    Math.hypot(points[a].x - points[b].x, points[a].y - points[b].y);

  const faceWidth = dist(LANDMARKS.faceLeft, LANDMARKS.faceRight);
  const faceLength = dist(LANDMARKS.faceTop, LANDMARKS.faceChin);
  const foreheadWidth = dist(LANDMARKS.foreheadLeft, LANDMARKS.foreheadRight);
  const jawWidth = dist(LANDMARKS.jawLeft, LANDMARKS.jawRight);
  const cheekWidth = dist(LANDMARKS.cheekLeft, LANDMARKS.cheekRight);

  const measurements = [faceWidth, faceLength, foreheadWidth, jawWidth, cheekWidth];
  if (faceWidth === 0 || faceLength === 0 || !measurements.every(Number.isFinite)) {
    return null;
  }

  const lengthRatio = faceLength / faceWidth;
  const foreheadRatio = foreheadWidth / faceWidth;
  const jawRatio = jawWidth / faceWidth;
  const cheekRatio = cheekWidth / faceWidth;

  if (lengthRatio > 1.48) return "Oblong";
  if (jawRatio < 0.72 && foreheadRatio < cheekRatio * 0.94) return "Diamond";
  if (jawRatio < 0.76 && foreheadRatio > cheekRatio * 0.94) return "Heart";
  if (
    lengthRatio < 1.22 &&
    Math.abs(faceWidth - faceLength) / faceLength < 0.16
  ) {
    return "Round";
  }
  if (jawRatio > 0.82 && lengthRatio < 1.48) return "Square";
  return "Oval";
}

/* =========================
   COMPONENT
========================= */

export default function FaceAnalysis({ onClose }) {
  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const streamRef = useRef(null);

  const [landmarker, setLandmarker] = useState(null);
  const [loading, setLoading] = useState(true);
  const [image, setImage] = useState(null);
  const [cameraActive, setCameraActive] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState(null);

  const stopCamera = useCallback(() => {
    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;
    setCameraActive(false);
  }, []);

  // Load the model (cached after the first time)
  useEffect(() => {
    let cancelled = false;

    getLandmarker()
      .then((lm) => {
        if (!cancelled) setLandmarker(lm);
      })
      .catch((err) => {
        console.error(err);
        if (!cancelled) setError(MSG.loadFailed);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  // On close: stop the camera and restore page scroll
  useEffect(() => {
    const previouslyFocused = document.activeElement;
    document.body.style.overflow = "hidden";

    return () => {
      streamRef.current?.getTracks().forEach((track) => track.stop());
      document.body.style.overflow = "";
      previouslyFocused?.focus?.();
    };
  }, []);

  // Escape closes the panel
  useEffect(() => {
    const onKey = (e) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [onClose]);

  // Attach the stream once the <video> element has mounted
  useEffect(() => {
    if (cameraActive && videoRef.current && streamRef.current) {
      videoRef.current.srcObject = streamRef.current;
    }
  }, [cameraActive]);

  const openCamera = async () => {
    try {
      setError("");
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: "user" },
        audio: false,
      });
      streamRef.current = stream;
      setCameraActive(true);
    } catch (err) {
      console.error(err);
      setError(MSG.cameraFailed);
    }
  };

  const capturePhoto = () => {
    const video = videoRef.current;
    const canvas = canvasRef.current;
    if (!video || !canvas || !video.videoWidth) return;

    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    canvas.getContext("2d").drawImage(video, 0, 0);

    setImage(canvas.toDataURL("image/jpeg"));
    setResult(null);
    stopCamera();
  };

  const uploadPhoto = (event) => {
    const file = event.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith("image/")) {
      setError(MSG.notImage);
      return;
    }
    if (file.size > MAX_FILE_BYTES) {
      setError(MSG.tooLarge);
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      setImage(reader.result);
      setResult(null);
      setError("");
    };
    reader.onerror = () => setError(MSG.unreadable);
    reader.readAsDataURL(file);
  };

  const resetPhoto = () => {
    setImage(null);
    setResult(null);
    setError("");
  };

  const analyzeFace = () => {
    if (!image || !landmarker) {
      setError(MSG.notReady);
      return;
    }

    setError("");

    const img = new Image();

    img.onload = () => {
      try {
        const detection = landmarker.detect(img);

        if (!detection.faceLandmarks?.length) {
          setError(MSG.noFace);
          return;
        }

        const shape = classifyFaceShape(detection.faceLandmarks[0]);
        if (!shape) {
          setError(MSG.unmeasurable);
          return;
        }

        setResult({ shape, ...FACE_SHAPES[shape] });
      } catch (err) {
        console.error(err);
        setError(MSG.analysisFailed);
      }
    };

    img.onerror = () => setError(MSG.unreadable);
    img.src = image;
  };

  return (
    <div className="face-analysis-overlay">
      <div
        className="face-analysis-panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby="face-analysis-title"
      >
        <div className="face-analysis-header">
          <div>
            <span className="eyebrow">NMK OPTICALS</span>
            <h2 id="face-analysis-title">Find Your Perfect Frame</h2>
          </div>

          <button onClick={onClose} aria-label="Close face analysis" type="button">
            <X />
          </button>
        </div>

        <p className="face-analysis-intro">
          Discover your estimated face shape and explore frame styles that may
          complement it.
        </p>

        {loading && <p role="status">Loading face analyzer...</p>}

        {!image && !cameraActive && (
          <div className="face-upload-options">
            <label className="face-upload-btn" style={{ position: "relative" }}>
              <Upload />
              Upload Photo
              <input
                type="file"
                accept="image/*"
                onChange={uploadPhoto}
                style={VISUALLY_HIDDEN}
              />
            </label>

            <button className="face-camera-btn" type="button" onClick={openCamera}>
              <Camera /> Open Camera
            </button>
          </div>
        )}

        {cameraActive && (
          <div className="face-camera-area">
            <video ref={videoRef} autoPlay playsInline muted />

            <button type="button" onClick={capturePhoto}>
              Capture Photo
            </button>
          </div>
        )}

        <canvas ref={canvasRef} hidden />

        {image && (
          <div className="face-photo-area">
            <img src={image} alt="Selected face" />

            <div className="face-photo-actions">
              <button type="button" onClick={resetPhoto}>
                Choose Another Photo
              </button>

              <button
                type="button"
                className="face-analyze-btn"
                onClick={analyzeFace}
                disabled={loading || !landmarker}
              >
                <ScanFace size={17} /> Analyze My Face
              </button>
            </div>
          </div>
        )}

        {error && (
          <p role="alert" className="face-error">
            {error}
          </p>
        )}

        {result && (
          <div className="face-result" aria-live="polite">
            <span className="eyebrow">YOUR ANALYSIS</span>

            <h3>{result.shape} Face Shape</h3>

            <p>{result.description}</p>

            <h4>Recommended Frame Styles</h4>

            <div className="recommended-frames">
              {result.frames.map((frame) => (
                <span key={frame}>{frame}</span>
              ))}
            </div>

            <p className="face-disclaimer">
              This is an automated estimate based on facial proportions, not a
              professional optical or medical assessment. Try different frames
              to decide what you like best.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}