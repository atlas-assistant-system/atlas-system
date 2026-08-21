const AtlasFaceQuality = (() => {
    const DETECTOR_MIN_FACE_SIZE = 224;
    const ENROLLMENT_MIN_FACE_SIZE = 300;
    const CAPTURE_MIN_FACE_SIZE = 340;
    const MAX_CAPTURE_ANGLE = .5;
    const MAX_NEUTRAL_OFFSET = .12;
    const SIDE_POSE_OFFSET = Math.PI / 12;
    const VERTICAL_POSE_OFFSET = Math.PI / 18;
    const POSE_HYSTERESIS = .07;

    function angle(face) {
        const value = face?.rotation?.angle;
        return {
            yaw: Number(value?.yaw) || 0,
            pitch: Number(value?.pitch) || 0,
            roll: Number(value?.roll) || 0,
        };
    }

    function handOccludesFace(face, handResult) {
        const box = face?.boxRaw;
        const hands = handResult?.landmarks || [];
        if (!box || box.length < 4 || !hands.length) {
            return false;
        }

        const [x, y, width, height] = box;
        const inside = point => point.x >= x + width * .12 && point.x <= x + width * .88
            && point.y >= y + height * .12 && point.y <= y + height * .92;
        return hands.some(points => points.filter(inside).length >= 3);
    }

    function isNeutral(face, center = null) {
        const current = angle(face);
        const origin = center || { yaw: 0, pitch: 0, roll: 0 };
        return Math.abs(current.yaw - origin.yaw) <= MAX_NEUTRAL_OFFSET
            && Math.abs(current.pitch - origin.pitch) <= (center ? MAX_NEUTRAL_OFFSET : .2)
            && Math.abs(current.roll - origin.roll) <= MAX_NEUTRAL_OFFSET;
    }

    function evaluate(result, options = {}) {
        if (!result || result.face?.length !== 1) {
            return { ready: false, reason: result?.face?.length > 1
                ? 'Debe aparecer una sola persona.' : 'No se detecta ningún rostro.' };
        }

        const face = result.face[0];
        const confidence = face.faceScore || face.boxScore || 0;
        const minSize = options.minSize ?? CAPTURE_MIN_FACE_SIZE;
        if (confidence < (options.minConfidence ?? .6)) {
            return { ready: false, face, reason: 'La detección facial no tiene suficiente confianza.' };
        }
        if (!face.box || Math.min(face.box[2], face.box[3]) < minSize) {
            return { ready: false, face, reason: `Acércate hasta que el rostro mida al menos ${minSize} px.` };
        }
        if (!face.embedding?.length) {
            return { ready: false, face, reason: 'La firma facial todavía no está disponible.' };
        }
        if (handOccludesFace(face, options.handResult)) {
            return { ready: false, face, occluded: true, reason: 'Aparta la mano de los ojos, la nariz y la boca.' };
        }

        const rotation = angle(face);
        if (options.neutral === false) {
            if (Math.max(Math.abs(rotation.yaw), Math.abs(rotation.pitch), Math.abs(rotation.roll))
                > (options.maxAngle ?? MAX_CAPTURE_ANGLE)) {
                return { ready: false, face, reason: 'La cabeza está demasiado girada para obtener una firma fiable.' };
            }
        } else if (!isNeutral(face, options.center)) {
            return { ready: false, face, reason: 'Vuelve a mirar de frente a la cámara.' };
        }

        const passiveWarning = (face.real ?? 0) < .6 || (face.live ?? 0) < .6;
        return {
            ready: true,
            face,
            occluded: false,
            passiveWarning,
            reason: passiveWarning ? 'Rostro preparado; señales pasivas inestables.' : 'Rostro preparado.',
        };
    }

    function calibration(faces) {
        if (!faces?.length) {
            return null;
        }
        const angles = faces.map(angle);
        const mean = key => angles.reduce((total, item) => total + item[key], 0) / angles.length;
        return { yaw: mean('yaw'), pitch: mean('pitch'), roll: mean('roll') };
    }

    function offsets(face, center) {
        const current = angle(face);
        const origin = center || { yaw: 0, pitch: 0, roll: 0 };
        return {
            yaw: current.yaw - origin.yaw,
            pitch: current.pitch - origin.pitch,
            roll: current.roll - origin.roll,
        };
    }

    function poseState(face, pose, center, active = false, firstSide = 0) {
        if (!center) {
            return { matches: pose === 'front' && isNeutral(face), progress: 0, target: 0, aligned: true };
        }
        const movement = offsets(face, center);
        const { yaw, pitch, roll } = movement;
        if (pose === 'front') {
            const target = active ? .11 : .08;
            const progress = Math.max(Math.abs(yaw), Math.abs(pitch), Math.abs(roll));
            return { matches: progress <= target, progress, target, aligned: true, movement };
        }
        const horizontal = ['side', 'opposite', 'left', 'right'].includes(pose);
        const entryTarget = horizontal ? SIDE_POSE_OFFSET : VERTICAL_POSE_OFFSET;
        const target = active ? entryTarget - POSE_HYSTERESIS : entryTarget;
        const progress = pose === 'side' ? Math.abs(yaw)
            : pose === 'opposite' ? Math.max(0, -yaw * firstSide)
                : pose === 'left' ? Math.max(0, -yaw)
                    : pose === 'right' ? Math.max(0, yaw)
                        : pose === 'up' ? Math.max(0, -pitch) : Math.max(0, pitch);
        const aligned = horizontal ? Math.abs(pitch) <= .3 : Math.abs(yaw) <= .3;
        return { matches: aligned && progress >= target, progress, target: entryTarget, aligned, movement };
    }

    function matchesPose(face, pose, center, active = false, firstSide = 0) {
        return poseState(face, pose, center, active, firstSide).matches;
    }

    function captureScore(face, center) {
        const current = angle(face);
        const origin = center || { yaw: 0, pitch: 0, roll: 0 };
        const posePenalty = Math.abs(current.yaw - origin.yaw)
            + Math.abs(current.pitch - origin.pitch) + Math.abs(current.roll - origin.roll);
        const size = face?.box ? Math.min(face.box[2], face.box[3]) : 0;
        return (face?.faceScore || face?.boxScore || 0) + Math.min(2, size / CAPTURE_MIN_FACE_SIZE)
            - posePenalty * 3;
    }

    function averageDescriptors(faces, center = null, limit = 3) {
        const selected = faces.toSorted((first, second) => captureScore(second, center) - captureScore(first, center))
            .slice(0, limit);
        const dimension = selected[0]?.embedding?.length || 0;
        if (!dimension || selected.some(face => face.embedding?.length !== dimension)) {
            throw new Error('Las capturas faciales no contienen firmas compatibles.');
        }

        const descriptor = Array.from({ length: dimension }, (_, index) =>
            selected.reduce((total, face) => total + face.embedding[index], 0) / selected.length);
        const norm = Math.hypot(...descriptor);
        return norm ? descriptor.map(value => value / norm) : descriptor;
    }

    function selfCheck() {
        const face = {
            faceScore: .9, box: [0, 0, 400, 400], boxRaw: [.25, .1, .5, .7],
            embedding: [1, 0], rotation: { angle: { yaw: 0, pitch: 0, roll: 0 } }, real: .1, live: .1,
        };
        const result = { face: [face] };
        const coveringHand = { landmarks: [[{ x: .5, y: .3 }, { x: .51, y: .31 }, { x: .52, y: .32 }]] };
        if (!evaluate(result).ready || evaluate(result, { handResult: coveringHand }).ready
            || matchesPose({ ...face, rotation: { angle: { yaw: SIDE_POSE_OFFSET - .001, pitch: 0, roll: 0 } } }, 'side', angle(face))
            || !matchesPose({ ...face, rotation: { angle: { yaw: -SIDE_POSE_OFFSET, pitch: 0, roll: 0 } } }, 'side', angle(face))
            || !matchesPose({ ...face, rotation: { angle: { yaw: SIDE_POSE_OFFSET, pitch: 0, roll: 0 } } }, 'opposite', angle(face), false, -1)) {
            throw new Error('AtlasFaceQuality self-check failed.');
        }
    }

    return Object.freeze({
        DETECTOR_MIN_FACE_SIZE, ENROLLMENT_MIN_FACE_SIZE, CAPTURE_MIN_FACE_SIZE, MAX_CAPTURE_ANGLE,
        angle, offsets, handOccludesFace, isNeutral, evaluate, calibration, poseState, matchesPose,
        averageDescriptors, selfCheck,
    });
})();

if (typeof window !== 'undefined') {
    window.AtlasFaceQuality = AtlasFaceQuality;
}
if (typeof module !== 'undefined') {
    module.exports = AtlasFaceQuality;
}
