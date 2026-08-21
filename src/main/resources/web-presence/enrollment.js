/**
 * El onboarding biométrico: las cinco poses, el aguante que se le exige a cada una y el bucle
 * que espera a que el rostro se mantenga quieto antes de quedarse con la captura.
 *
 * Lo usan el espejo (`web-core/app.js`) y la página de mantenimiento de presence, que hacen el
 * mismo alta con distinta piel. Lo que cambia entre ambos —de dónde sale el último resultado de
 * la cámara y dónde se guarda la pose neutral— entra por `bind`.
 */
const AtlasEnrollment = (() => {
    const ENROLLMENT_POSES = [
        { id: 'front', instruction: 'Mira de frente' },
        { id: 'side', instruction: 'Gira ligeramente hacia el lado que prefieras' },
        { id: 'opposite', instruction: 'Ahora gira ligeramente hacia el otro lado' },
        { id: 'up', instruction: 'Mira ligeramente hacia arriba' },
        { id: 'down', instruction: 'Mira ligeramente hacia abajo' },
    ];
    const ENROLLMENT_HOLD_FRAMES = 2;
    const ENROLLMENT_FRONT_FRAMES = 4;
    const FACE_RESULT_MAX_AGE_MS = 750;
    const POSE_TIMEOUT_MS = 20000;
    const MISSED_FRAMES_BEFORE_RESET = 3;

    function sleep(milliseconds) {
        return new Promise(resolve => setTimeout(resolve, milliseconds));
    }

    /** Un resultado viejo es un rostro que ya no está delante: la cámara dejó de dar frames. */
    function faceResultIsRecent(result) {
        const age = Date.now() - Number(result?.timestamp);
        return Number.isFinite(age) && age >= 0 && age <= FACE_RESULT_MAX_AGE_MS;
    }

    function showEnrollmentStep(active) {
        document.querySelectorAll('.enrollment-steps li').forEach((node, index) => {
            node.classList.toggle('active', index === active);
            node.classList.toggle('complete', index < active);
            if (index === active) {
                node.setAttribute('aria-current', 'step');
            } else {
                node.removeAttribute('aria-current');
            }
        });
    }

    function enrollmentPoseFeedback(status, pose, poseState, stableFrames, requiredFrames) {
        if (!status.ready) {
            return status.reason;
        }
        if (stableFrames > 0) {
            return `Mantén la posición · confirmando ${stableFrames}/${requiredFrames}`;
        }
        if (pose.id === 'front') {
            return 'Mantén el rostro centrado un instante';
        }
        if (!poseState.aligned) {
            return pose.id === 'side' || pose.id === 'opposite'
                ? `${pose.instruction} · mantén la cabeza nivelada`
                : `${pose.instruction} · evita girar hacia un lado`;
        }
        const progress = Math.floor(poseState.progress * 1800 / Math.PI) / 10;
        const target = Math.round(poseState.target * 180 / Math.PI);
        return `${pose.instruction} · ${progress.toFixed(1)}° / ${target}°`;
    }

    /**
     * `faceStatus` evalúa la calidad del frame actual como la entiende cada página, y `center`
     * lee y escribe la pose neutral: la calibra la primera pose y la usan las cuatro siguientes.
     */
    function bind({ faceStatus, center }) {
        async function capture(currentResult, currentVersion, onStep) {
            const descriptors = [];
            let firstSide = 0;
            center.set(null);
            for (const [index, pose] of ENROLLMENT_POSES.entries()) {
                onStep(index, pose);
                let lastVersion = -1;
                let stableFaces = [];
                let missedFrames = 0;
                const requiredFrames = pose.id === 'front' ? ENROLLMENT_FRONT_FRAMES : ENROLLMENT_HOLD_FRAMES;
                const expiresAt = Date.now() + POSE_TIMEOUT_MS;
                while (Date.now() < expiresAt) {
                    const version = currentVersion();
                    if (version !== lastVersion) {
                        lastVersion = version;
                        const result = currentResult();
                        const status = faceStatus(result, {
                            neutral: false, minSize: AtlasFaceQuality.ENROLLMENT_MIN_FACE_SIZE,
                        });
                        const poseState = status.ready ? AtlasFaceQuality.poseState(
                            status.face, pose.id, center.get(), stableFaces.length > 0, firstSide) : null;
                        if (poseState?.matches) {
                            stableFaces.push(status.face);
                            missedFrames = 0;
                        } else if (++missedFrames >= MISSED_FRAMES_BEFORE_RESET) {
                            stableFaces = [];
                            missedFrames = 0;
                        }
                        onStep(index, pose, enrollmentPoseFeedback(
                            status, pose, poseState, stableFaces.length, requiredFrames));
                        if (stableFaces.length >= requiredFrames) {
                            if (pose.id === 'front') {
                                center.set(AtlasFaceQuality.calibration(stableFaces));
                            } else if (pose.id === 'side') {
                                firstSide = Math.sign(AtlasFaceQuality.offsets(status.face, center.get()).yaw) || 1;
                            }
                            descriptors.push(AtlasFaceQuality.averageDescriptors(stableFaces, center.get()));
                            break;
                        }
                    }
                    await sleep(60);
                }
                if (descriptors.length !== index + 1) {
                    throw new Error(`No se pudo capturar: ${pose.instruction.toLowerCase()}.`);
                }
            }
            return descriptors;
        }

        return { capture };
    }

    return {
        ENROLLMENT_POSES, ENROLLMENT_HOLD_FRAMES, ENROLLMENT_FRONT_FRAMES, FACE_RESULT_MAX_AGE_MS,
        sleep, faceResultIsRecent, showEnrollmentStep, enrollmentPoseFeedback, bind,
    };
})();
