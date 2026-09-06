/**
 * La geometría de la mano: qué dedos están extendidos, hacia dónde apunta la palma y cuándo
 * dos dedos se juntan en un pinch. Las funciones son puras —entran landmarks, salen números y
 * booleanos— y no tocan ni el DOM ni el estado de la página.
 *
 * Cada predicado tiene al lado su `*Metrics`, que devuelve los números con los que decide. El
 * espejo solo necesita el sí o el no; `/presence/sandbox` enseña también la medida, y así mide
 * exactamente el mismo código que se ejecuta en producción en vez de una copia.
 */
const AtlasGestures = (() => {
    const HAND_CONFIDENCE = .65;
    const GESTURE_HOLD_MS = 180;
    const PINCH_HOLD_MS = 100;
    const PINCH_DISTANCE_RATIO = .4;
    const EXTENDED_BASE_ANGLE = 155;
    const EXTENDED_TIP_ANGLE = 145;
    const EXTENDED_REACH = 1.12;
    const TOGETHER_RATIO = .55;
    const DIRECTION_DEAD_ZONE = .65;
    const DIRECTION_DOMINANCE = 1.2;

    function pointCoordinates(point) {
        return Array.isArray(point) ? { x: point[0], y: point[1], z: point[2] || 0 } : point;
    }

    function pointDistance(first, second) {
        const a = pointCoordinates(first);
        const b = pointCoordinates(second);
        return Math.hypot(a.x - b.x, a.y - b.y, (a.z || 0) - (b.z || 0));
    }

    function jointAngle(first, middle, last) {
        const a = pointCoordinates(first);
        const b = pointCoordinates(middle);
        const c = pointCoordinates(last);
        const ab = Math.hypot(a.x - b.x, a.y - b.y, (a.z || 0) - (b.z || 0));
        const cb = Math.hypot(c.x - b.x, c.y - b.y, (c.z || 0) - (b.z || 0));
        if (!ab || !cb) {
            return 0;
        }
        const cosine = ((a.x - b.x) * (c.x - b.x) + (a.y - b.y) * (c.y - b.y)
            + ((a.z || 0) - (b.z || 0)) * ((c.z || 0) - (b.z || 0))) / (ab * cb);
        return Math.acos(Math.max(-1, Math.min(1, cosine))) * 180 / Math.PI;
    }

    function fingerMetrics(points, name, base) {
        const angle1 = jointAngle(points[base], points[base + 1], points[base + 3]);
        const angle2 = jointAngle(points[base + 1], points[base + 2], points[base + 3]);
        const reach = pointDistance(points[0], points[base + 3]) / pointDistance(points[0], points[base + 1]);
        return {
            name, angle1, angle2, reach,
            extended: angle1 > EXTENDED_BASE_ANGLE && angle2 > EXTENDED_TIP_ANGLE && reach > EXTENDED_REACH,
        };
    }

    function fingerIsExtended(points, base) {
        return fingerMetrics(points, '', base).extended;
    }

    function pinchMetrics(points) {
        const palmWidth = pointDistance(points[5], points[17]);
        const distance = pointDistance(points[4], points[8]);
        return { palmWidth, distance, ratio: palmWidth ? distance / palmWidth : Infinity };
    }

    function isPinch(points) {
        return pinchMetrics(points).ratio <= PINCH_DISTANCE_RATIO;
    }

    function isDirectionalPose(points) {
        const palmWidth = pointDistance(points[5], points[17]);
        return fingerIsExtended(points, 5) && fingerIsExtended(points, 9)
            && !fingerIsExtended(points, 13) && !fingerIsExtended(points, 17)
            && palmWidth > 0 && pointDistance(points[8], points[12]) <= palmWidth * TOGETHER_RATIO;
    }

    function isPointingPose(points) {
        return fingerIsExtended(points, 5)
            && !fingerIsExtended(points, 9)
            && !fingerIsExtended(points, 13)
            && !fingerIsExtended(points, 17);
    }

    function isOpenPalmPose(points) {
        return [5, 9, 13, 17].every(base => fingerIsExtended(points, base));
    }

    function directionMetrics(points) {
        const indexBase = pointCoordinates(points[5]);
        const middleBase = pointCoordinates(points[9]);
        const indexTip = pointCoordinates(points[8]);
        const middleTip = pointCoordinates(points[12]);
        const dx = (indexBase.x + middleBase.x - indexTip.x - middleTip.x) / 2;
        const dy = (indexTip.y + middleTip.y - indexBase.y - middleBase.y) / 2;
        const palmWidth = pointDistance(points[5], points[17]);
        return { dx, dy, magnitude: Math.hypot(dx, dy), deadZone: palmWidth * DIRECTION_DEAD_ZONE };
    }

    function staticDirection(points) {
        const { dx, dy, magnitude, deadZone } = directionMetrics(points);
        if (magnitude < deadZone) {
            return null;
        }
        if (Math.abs(dx) > Math.abs(dy) * DIRECTION_DOMINANCE) {
            return dx > 0 ? 'PALM_RIGHT' : 'PALM_LEFT';
        }
        if (Math.abs(dy) > Math.abs(dx) * DIRECTION_DOMINANCE) {
            return dy > 0 ? 'PALM_DOWN' : 'PALM_UP';
        }
        return null;
    }

    /** Manos sintéticas: si los umbrales dejan de reconocer una palma abierta o una dirección,
     *  esto revienta al cargar la página en vez de dejar el gesto muerto en el espejo. */
    function selfCheck() {
        const hand = (rotate, folded) => {
            const points = Array.from({ length: 21 }, () => ({ x: 0, y: 0, z: 0 }));
            [5, 9, 13, 17].forEach((base, index) => {
                const x = (index - 1.5) * .2;
                [0, 1, 2, 3].forEach(offset => {
                    const y = .2 + offset * .2;
                    points[base + offset] = rotate ? { x: y, y: -x, z: 0 } : { x, y, z: 0 };
                });
            });
            folded.forEach(([tip, knuckle]) => points[tip] = points[knuckle]);
            return points;
        };
        const openPalm = hand(false, []);
        const twoFingers = hand(false, [[16, 14], [20, 18]]);
        const check = (condition, what) => {
            if (!condition) {
                throw new Error(`AtlasGestures self-check failed: ${what}.`);
            }
        };
        check(isOpenPalmPose(openPalm), 'open palm');
        check(!isOpenPalmPose(hand(false, [[12, 10]])), 'folded finger is not an open palm');
        check(isDirectionalPose(twoFingers), 'two fingers together');
        check(staticDirection(twoFingers) === 'PALM_DOWN', 'PALM_DOWN');
        check(staticDirection(hand(true, [[16, 14], [20, 18]])) === 'PALM_LEFT', 'PALM_LEFT');
    }

    /** El desafío de la prueba de vida: hoy siempre es cerrar el puño. */
    function challengeDetector(type) {
        return result => type === 'FIST'
            && result?.gestures?.[0]?.some(value => value.categoryName === 'Closed_Fist'
                && value.score >= HAND_CONFIDENCE);
    }

    return {
        HAND_CONFIDENCE, GESTURE_HOLD_MS, PINCH_HOLD_MS, PINCH_DISTANCE_RATIO,
        challengeDetector,
        pointCoordinates, pointDistance, jointAngle,
        fingerMetrics, fingerIsExtended,
        pinchMetrics, isPinch,
        isDirectionalPose, isPointingPose, isOpenPalmPose,
        directionMetrics, staticDirection,
        selfCheck,
    };
})();
