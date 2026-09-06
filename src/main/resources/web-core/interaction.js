const AtlasInteraction = (() => {
    const { pointDistance } = AtlasGestures;

    const INTERACTIVE = 'button:not(:disabled), input:not(:disabled), textarea:not(:disabled), '
        + 'select:not(:disabled), label:has(input:not(:disabled)), .clickable, .day, .item';
    const VOICE_LANGUAGE = 'es-ES';
    const MAGNET_RADIUS = 120;
    const HYSTERESIS = 0.75;
    const ADJUST_WINDOW_MS = 700;

    const pointer = { target: null, candidate: null, frames: 0, position: null, missing: 0 };
    let voice = { recognition: null, target: null, listening: false, preparing: false };
    let voiceRequest = 0;
    let interactionStatusHandle = null;
    let config = null;
    let targetCache = null;
    let dictationTarget = null;
    const adjustment = { type: null, at: 0, count: 0 };

    function bind(options) {
        config = options;
        updateVoiceControl();
        document.getElementById('voice-control').addEventListener('click', toggleVoiceInput);
        document.addEventListener('focusin', event => {
            if (isTextField(event.target)) {
                voice.target = event.target;
            }
        });
        addEventListener('scroll', invalidateTargets, { capture: true, passive: true });
        addEventListener('resize', invalidateTargets);
        new MutationObserver(mutations => {
            if (mutations.some(mutation => !isChrome(mutation.target))) {
                invalidateTargets();
            }
        }).observe(document.body, {
            childList: true, subtree: true, attributes: true,
            attributeFilter: ['hidden', 'disabled', 'class', 'style'],
        });
    }

    function isChrome(node) {
        const element = node instanceof Element ? node : node.parentElement;
        return Boolean(element?.closest('#gesture-pointer, #interaction-status'));
    }

    function invalidateTargets() {
        targetCache = null;
    }

    function targets() {
        if (!targetCache) {
            targetCache = [...document.querySelectorAll(INTERACTIVE)]
                .map(element => ({ element, rect: element.getBoundingClientRect() }))
                .filter(candidate => candidate.rect.width > 0 && candidate.rect.height > 0);
        }
        return targetCache;
    }

    function reset() {
        stopVoiceInput();
        setPointerTarget(null);
        pointer.position = null;
        pointer.missing = 0;
        document.getElementById('gesture-pointer').classList.remove('visible');
    }

    function track(result, observed) {
        const point = result?.landmarks?.[0]?.[8];
        const mapped = point && cameraPoint(point);
        const element = document.getElementById('gesture-pointer');
        if (!mapped) {
            if (++pointer.missing >= 3) {
                element.classList.remove('visible');
                element.classList.remove('recognized');
                element.dataset.gesture = 'TRACKING';
                pointer.position = null;
                setPointerTarget(null);
            }
            return;
        }

        pointer.missing = 0;
        const position = smoothPointer(pointer.position, mapped);
        pointer.position = position;
        element.style.transform = `translate(${position.x}px, ${position.y}px)`;
        element.classList.add('visible');
        element.classList.toggle('recognized', Boolean(observed));
        element.dataset.gesture = observed ? observed.type.replace('PALM_', '') : 'TRACKING';
        const hit = document.elementFromPoint(position.x, position.y);
        const direct = hit && hit.closest(INTERACTIVE);
        trackPointerTarget(direct || nearestTarget(position, targets(), pointer.target));
    }

    function rectDistance(position, rect) {
        const dx = Math.max(rect.left - position.x, 0, position.x - rect.right);
        const dy = Math.max(rect.top - position.y, 0, position.y - rect.bottom);
        return Math.hypot(dx, dy);
    }

    function nearestTarget(position, candidates, current) {
        let best = null;
        let bestDistance = Infinity;
        let currentDistance = Infinity;
        for (const candidate of candidates) {
            const distance = rectDistance(position, candidate.rect);
            if (candidate.element === current) {
                currentDistance = distance;
            }
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate.element;
            }
        }
        if (currentDistance <= MAGNET_RADIUS && bestDistance > currentDistance * HYSTERESIS) {
            return current;
        }
        return bestDistance <= MAGNET_RADIUS ? best : null;
    }

    function smoothPointer(previous, current) {
        if (!previous) {
            return current;
        }
        const alpha = Math.max(0.25, Math.min(0.7, pointDistance(previous, current) / 120));
        return {
            x: previous.x + (current.x - previous.x) * alpha,
            y: previous.y + (current.y - previous.y) * alpha,
        };
    }

    function trackPointerTarget(target) {
        if (pointer.candidate !== target) {
            pointer.candidate = target;
            pointer.frames = 1;
            return;
        }
        if (++pointer.frames >= 2) {
            setPointerTarget(target);
        }
    }

    function cameraPoint(point) {
        const video = config.video;
        if (!video.videoWidth || !video.videoHeight) {
            return null;
        }

        let x = Array.isArray(point) ? point[0] : point.x;
        let y = Array.isArray(point) ? point[1] : point.y;
        if (!Number.isFinite(x) || !Number.isFinite(y)) {
            return null;
        }
        if (x <= 1 && y <= 1) {
            x *= video.videoWidth;
            y *= video.videoHeight;
        }

        const scale = Math.max(innerWidth / video.videoWidth, innerHeight / video.videoHeight);
        const offsetX = (innerWidth - video.videoWidth * scale) / 2;
        const offsetY = (innerHeight - video.videoHeight * scale) / 2;

        return {
            x: Math.max(0, Math.min(innerWidth, innerWidth - (offsetX + x * scale))),
            y: Math.max(0, Math.min(innerHeight, offsetY + y * scale)),
        };
    }

    function setPointerTarget(target) {
        if (pointer.target === target) {
            return;
        }
        if (pointer.target) {
            pointer.target.classList.remove('gesture-target');
        }
        pointer.target = target;
        pointer.candidate = target;
        pointer.frames = 0;
        if (isTextField(target)) {
            voice.target = target;
        }
        if (target) {
            target.classList.add('gesture-target');
        }
    }

    function apply(type) {
        if (type === 'POINT') {
            if (pointer.target && typeof pointer.target.focus === 'function') {
                pointer.target.focus({ preventScroll: true });
            }
            return;
        }
        if (type === 'PINCH') {
            activatePointerTarget();
            return;
        }
        if (type === 'PALM_LEFT' || type === 'PALM_RIGHT') {
            config.shiftView(type === 'PALM_LEFT' ? 1 : -1);
            return;
        }
        if (type === 'PALM_UP' || type === 'PALM_DOWN') {
            if (!adjustFocusedControl(type)) {
                scrollByGesture(type);
            }
            return;
        }
        if (type === 'OPEN_PALM') {
            if (voice.listening || voice.preparing) {
                stopVoiceInput();
            } else {
                config.onCancel();
            }
            return;
        }
        if (type === 'THUMBS_DOWN') {
            config.toggleMirror();
            return;
        }
        if (type === 'THUMBS_UP') {
            const form = document.activeElement && document.activeElement.closest('form');
            if (form) {
                form.requestSubmit();
            }
        }
    }

    function activatePointerTarget() {
        const target = pointer.target;
        if (!target) {
            startVoiceInput(null);
            return;
        }
        const field = isTextField(target) ? target
            : target instanceof HTMLLabelElement && isTextField(target.control) ? target.control
                : null;
        if (field) {
            field.focus({ preventScroll: true });
            startVoiceInput(field);
            return;
        }
        target.focus({ preventScroll: true });
        target.click();
    }

    function isTextField(target) {
        return target instanceof HTMLTextAreaElement
            || target instanceof HTMLInputElement && ['text', 'search', 'email', 'tel', 'url'].includes(target.type);
    }

    function adjustFocusedControl(type) {
        const control = document.activeElement;
        const increment = type === 'PALM_UP' ? 1 : -1;
        if (control instanceof HTMLSelectElement) {
            control.selectedIndex = Math.max(0, Math.min(control.options.length - 1,
                control.selectedIndex + increment));
        } else if (control instanceof HTMLInputElement && ['date', 'time', 'number'].includes(control.type)) {
            const steps = control.type === 'number' ? adjustmentSteps(type) : 1;
            for (let step = 0; step < steps; step++) {
                increment > 0 ? control.stepUp() : control.stepDown();
            }
        } else {
            return false;
        }
        control.dispatchEvent(new Event('change', { bubbles: true }));
        setInteractionStatus(control.value);
        return true;
    }

    function adjustmentSteps(type) {
        const now = performance.now();
        if (adjustment.type !== type || now - adjustment.at > ADJUST_WINDOW_MS) {
            adjustment.count = 0;
        }
        adjustment.type = type;
        adjustment.at = now;
        adjustment.count++;
        return adjustment.count < 3 ? 1 : adjustment.count < 6 ? 5 : 10;
    }

    function scrollByGesture(type) {
        let container = config.scrollRoot() || pointer.target;
        while (container && container !== document.body
            && container.scrollHeight <= container.clientHeight) {
            container = container.parentElement;
        }
        if (!container || container === document.body) {
            container = config.scrollFallback();
        }
        const distance = Math.max(240, container.clientHeight * 0.7);
        container.scrollBy({ top: type === 'PALM_UP' ? distance : -distance, behavior: 'smooth' });
    }

    function setInteractionStatus(text) {
        const status = document.getElementById('interaction-status');
        status.textContent = text;
        status.classList.add('visible');
        clearTimeout(interactionStatusHandle);
        interactionStatusHandle = setTimeout(() => status.classList.remove('visible'), 2500);
    }

    async function startVoiceInput(target) {
        const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
        if (!Recognition) {
            setInteractionStatus('El dictado no está disponible en este navegador.');
            return;
        }
        stopVoiceInput();
        const request = voiceRequest;
        voice = { recognition: null, target, listening: false, preparing: true };
        updateVoiceControl();

        const local = await prepareOnDeviceVoice(Recognition);
        if (request !== voiceRequest) {
            return;
        }

        const recognition = new Recognition();
        recognition.lang = VOICE_LANGUAGE;
        recognition.continuous = false;
        recognition.interimResults = false;
        recognition.maxAlternatives = 1;
        if (local) {
            recognition.processLocally = true;
        }
        voice = { recognition, target, listening: true, preparing: false };
        recognition.onstart = () => {
            updateVoiceControl();
            setInteractionStatus('Escuchando…');
        };
        recognition.onresult = event => {
            const transcript = event.results[0][0].transcript;
            if (target) {
                appendDictation(target, transcript);
            } else {
                obeyCommand(transcript);
            }
        };
        recognition.onerror = event => {
            if (event.error !== 'aborted') {
                setInteractionStatus(voiceErrorMessage(event.error));
            }
        };
        recognition.onend = () => {
            if (voice.recognition === recognition) {
                voice = { recognition: null, target, listening: false, preparing: false };
                updateVoiceControl();
            }
        };
        try {
            recognition.start();
        } catch (_) {
            voice = { recognition: null, target, listening: false, preparing: false };
            updateVoiceControl();
            setInteractionStatus('No se pudo iniciar el dictado.');
        }
    }

    async function prepareOnDeviceVoice(Recognition) {
        if (!('processLocally' in Recognition.prototype)
            || typeof Recognition.available !== 'function'
            || typeof Recognition.install !== 'function') {
            return false;
        }

        try {
            const options = { langs: [VOICE_LANGUAGE], processLocally: true };
            const availability = await Recognition.available(options);
            if (availability === 'available') {
                return true;
            }
            if (availability === 'unavailable') {
                return false;
            }

            setInteractionStatus('Descargando el reconocimiento de voz en español…');
            return await Recognition.install({ langs: [VOICE_LANGUAGE] });
        } catch (_) {
            return false;
        }
    }

    function voiceErrorMessage(error) {
        return ({
            'not-allowed': 'Permite el micrófono en el navegador y vuelve a pulsar Dictar.',
            'service-not-allowed': 'Este navegador no permite usar su servicio de voz.',
            'audio-capture': 'No se encuentra un micrófono disponible.',
            'no-speech': 'No se ha detectado voz. Inténtalo de nuevo.',
            'network': 'No se puede acceder al reconocimiento de voz remoto. Actualiza Chrome para usar el dictado local.',
            'language-not-supported': 'El reconocimiento no admite español.',
        })[error] || 'No se pudo reconocer la voz.';
    }

    function toggleVoiceInput() {
        if (voice.listening || voice.preparing) {
            stopVoiceInput();
            return;
        }
        const target = isTextField(document.activeElement) ? document.activeElement
            : isTextField(pointer.target) ? pointer.target : voice.target;
        if (!target?.isConnected) {
            startVoiceInput(null);
            return;
        }
        target.focus({ preventScroll: true });
        startVoiceInput(target);
    }

    function obeyCommand(transcript) {
        const focused = document.activeElement;
        const number = parseSpokenNumber(transcript);
        if (number !== null && focused instanceof HTMLInputElement && focused.type === 'number') {
            focused.value = String(number);
            focused.dispatchEvent(new Event('input', { bubbles: true }));
            focused.dispatchEvent(new Event('change', { bubbles: true }));
            setInteractionStatus(focused.value);
            return;
        }
        const match = matchLabel(transcript, targets()
            .map(candidate => ({ element: candidate.element, label: labelOf(candidate.element) })));
        if (!match) {
            setInteractionStatus(`No te he entendido: ${transcript.trim()}`);
            return;
        }
        setPointerTarget(match);
        activatePointerTarget();
    }

    function labelOf(element) {
        return element.getAttribute('aria-label') || element.textContent.trim()
            || element.getAttribute('placeholder') || element.value || '';
    }

    function normalize(text) {
        return String(text ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '')
            .toLowerCase().replace(/[^a-z0-9.,\s-]/g, ' ').replace(/\s+/g, ' ').trim();
    }

    function matchLabel(transcript, candidates) {
        const text = normalize(transcript);
        if (!text) {
            return null;
        }
        const words = text.split(' ');
        let best = null;
        let bestScore = 0;
        let bestLength = Infinity;
        for (const candidate of candidates) {
            const label = normalize(candidate.label);
            if (!label) {
                continue;
            }
            const score = labelScore(text, words, label);
            if (score > bestScore || score === bestScore && score > 0 && label.length < bestLength) {
                best = candidate.element;
                bestScore = score;
                bestLength = label.length;
            }
        }
        return best;
    }

    function labelScore(text, words, label) {
        if (label === text) {
            return 3;
        }
        if (label.startsWith(text) || text.startsWith(label)) {
            return 2;
        }
        const labelWords = label.split(' ');
        const overlap = labelWords.filter(word => words.includes(word)).length / labelWords.length;
        return overlap >= 0.6 ? 1 + overlap : 0;
    }

    const WORD_NUMBERS = {
        cero: 0, un: 1, uno: 1, una: 1, dos: 2, tres: 3, cuatro: 4, cinco: 5, seis: 6, siete: 7,
        ocho: 8, nueve: 9, diez: 10, once: 11, doce: 12, trece: 13, catorce: 14, quince: 15,
        dieciseis: 16, diecisiete: 17, dieciocho: 18, diecinueve: 19, veinte: 20, veintiun: 21,
        veintiuno: 21, veintidos: 22, veintitres: 23, veinticuatro: 24, veinticinco: 25,
        veintiseis: 26, veintisiete: 27, veintiocho: 28, veintinueve: 29, treinta: 30,
        cuarenta: 40, cincuenta: 50, sesenta: 60, setenta: 70, ochenta: 80, noventa: 90,
        cien: 100, ciento: 100, doscientos: 200, trescientos: 300, cuatrocientos: 400,
        quinientos: 500, seiscientos: 600, setecientos: 700, ochocientos: 800, novecientos: 900,
        mil: 1000,
    };

    function parseSpokenNumber(transcript) {
        const text = normalize(transcript);
        if (!text) {
            return null;
        }
        const digits = text.match(/-?\d+(?:[.,]\d+)?/);
        if (digits) {
            return Number(digits[0].replace(',', '.'));
        }
        const [whole, fraction] = text.split(/\b(?:coma|punto)\b/);
        const value = wordsToNumber(whole);
        if (value === null) {
            return null;
        }
        if (fraction !== undefined) {
            const decimals = wordsToNumber(fraction);
            return decimals === null ? value : Number(`${value}.${decimals}`);
        }
        return /\bmedio\b/.test(text) ? value + 0.5 : value;
    }

    function wordsToNumber(text) {
        let total = 0;
        let found = false;
        for (const word of text.split(' ')) {
            const value = WORD_NUMBERS[word];
            if (value === undefined) {
                continue;
            }
            total = value === 1000 ? (total || 1) * 1000 : total + value;
            found = true;
        }
        return found ? total : null;
    }

    function updateVoiceControl() {
        const control = document.getElementById('voice-control');
        const supported = Boolean(window.SpeechRecognition || window.webkitSpeechRecognition);
        control.hidden = false;
        control.disabled = !supported || voice.preparing;
        control.classList.toggle('listening', voice.listening);
        control.setAttribute('aria-pressed', String(voice.listening));
        control.textContent = !supported ? 'Voz no disponible'
            : voice.preparing ? 'Preparando voz…'
                : voice.listening ? 'Detener voz' : 'Dictar';
    }

    function appendDictation(target, transcript) {
        if (dictationTarget !== target) {
            dictationTarget = target;
            target.value = '';
        }
        const start = target.selectionStart ?? target.value.length;
        const end = target.selectionEnd ?? target.value.length;
        const prefix = start > 0 && !/\s$/.test(target.value.slice(0, start)) ? ' ' : '';
        let value = target.value.slice(0, start) + prefix + transcript.trim() + target.value.slice(end);
        if (target.maxLength > 0) {
            value = value.slice(0, target.maxLength);
        }
        target.value = value;
        target.selectionStart = target.selectionEnd = Math.min(value.length, start + prefix.length + transcript.trim().length);
        target.dispatchEvent(new Event('input', { bubbles: true }));
        setInteractionStatus('Texto reconocido.');
    }

    function stopVoiceInput() {
        voiceRequest += 1;
        const target = voice.target;
        if (voice.recognition) {
            voice.recognition.abort();
        }
        voice = { recognition: null, target, listening: false, preparing: false };
        updateVoiceControl();
    }

    return {
        bind, reset, track, apply, status: setInteractionStatus,
        parseSpokenNumber, matchLabel, nearestTarget, MAGNET_RADIUS,
    };
})();
