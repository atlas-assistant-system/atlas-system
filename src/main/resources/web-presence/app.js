const MODEL_VERSION = 'human-faceres-3.3.6';
const MIN_CONFIDENCE = 0.6;
// ponytail: pinned CDN keeps this UI adapter small; serve the same assets locally for offline deployment.
const HUMAN_MODELS = 'https://cdn.jsdelivr.net/npm/@vladmandic/human@3.3.6/models/';

const state = {
    authentication: null,
    busy: false,
    cameraReady: false,
    human: null,
    result: null,
};

const dom = {
    authenticate: document.getElementById('authenticate'),
    camera: document.getElementById('camera'),
    cameraState: document.getElementById('camera-state'),
    challenge: document.getElementById('challenge'),
    challengeInstruction: document.getElementById('challenge-instruction'),
    challengeTime: document.getElementById('challenge-time'),
    closeSession: document.getElementById('close-session'),
    displayName: document.getElementById('display-name'),
    enroll: document.getElementById('enroll'),
    enrollmentCard: document.getElementById('enrollment-card'),
    events: document.getElementById('events'),
    feedback: document.getElementById('feedback'),
    openAgenda: document.getElementById('open-agenda'),
    profiles: document.getElementById('profiles'),
    sessionState: document.getElementById('session-state'),
    startCamera: document.getElementById('start-camera'),
};

async function load(path, options) {
    const response = await fetch(path, options);
    const body = response.status === 204 ? null : await response.json().catch(() => null);
    if (!response.ok) {
        throw body || { message: 'No se pudo completar la operación.' };
    }
    return body;
}

function send(path, value) {
    return load(path, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(value),
    });
}

function message(error) {
    return error && error.message ? error.message : 'No se pudo completar la operación.';
}

function setFeedback(text, error = false) {
    dom.feedback.textContent = text;
    dom.feedback.classList.toggle('error', error);
}

function updateActions() {
    const active = state.authentication && state.authentication.activeSession;
    const enrolled = state.authentication && state.authentication.enrolledProfiles > 0;
    dom.authenticate.disabled = state.busy || !state.cameraReady || !enrolled || Boolean(active);
    dom.enroll.disabled = state.busy || !state.cameraReady || !dom.displayName.value.trim();
    dom.closeSession.hidden = !active;
    dom.openAgenda.hidden = !active;
}

function faceStatus(result) {
    if (!result || result.face.length !== 1) {
        return { ready: false, reason: result && result.face.length > 1
            ? 'Debe aparecer una sola persona.' : 'Coloca tu rostro dentro de la guía.' };
    }

    const face = result.face[0];
    const confidence = face.faceScore || face.boxScore || 0;
    if (confidence < MIN_CONFIDENCE || Math.min(face.box[2], face.box[3]) < 160) {
        return { ready: false, reason: 'Acércate un poco y mira hacia la cámara.' };
    }
    if (!face.embedding || face.embedding.length === 0) {
        return { ready: false, reason: 'Calculando la firma facial…' };
    }
    if ((face.real || 0) < MIN_CONFIDENCE || (face.live || 0) < MIN_CONFIDENCE) {
        return { ready: false, reason: 'No se ha podido confirmar que sea un rostro real.' };
    }

    return { ready: true, face, reason: 'Rostro preparado.' };
}

async function startCamera() {
    if (!window.Human || !window.Human.Human) {
        setFeedback('No se pudo cargar el motor facial. Comprueba la conexión y recarga la página.', true);
        return;
    }

    state.busy = true;
    updateActions();
    dom.cameraState.textContent = 'Solicitando permiso para la cámara…';
    try {
        const stream = await navigator.mediaDevices.getUserMedia({
            audio: false,
            video: { facingMode: 'user', width: { ideal: 960 }, height: { ideal: 720 } },
        });
        dom.camera.srcObject = stream;
        await dom.camera.play();
        dom.cameraState.textContent = 'Cargando modelos faciales…';
        state.human = new window.Human.Human({
            backend: 'webgl',
            cacheSensitivity: 0.01,
            debug: false,
            modelBasePath: HUMAN_MODELS,
            filter: { enabled: true, equalization: true },
            face: {
                enabled: true,
                detector: { rotation: true, return: false, maxDetected: 1 },
                mesh: { enabled: true },
                iris: { enabled: true },
                description: { enabled: true },
                emotion: { enabled: true },
                antispoof: { enabled: true },
                liveness: { enabled: true },
            },
            body: { enabled: false },
            hand: { enabled: true, maxDetected: 1, landmarks: true },
            object: { enabled: false },
            segmentation: { enabled: false },
            gesture: { enabled: true },
        });
        await state.human.load();
        await state.human.warmup();
        state.cameraReady = true;
        dom.startCamera.hidden = true;
        detectLoop();
    } catch (error) {
        dom.cameraState.textContent = 'No se pudo iniciar la cámara.';
        setFeedback(error.message || 'Autoriza el uso de la cámara para continuar.', true);
    } finally {
        state.busy = false;
        updateActions();
    }
}

async function detectLoop() {
    if (!state.cameraReady) {
        return;
    }
    try {
        state.result = await state.human.detect(dom.camera);
        dom.cameraState.textContent = faceStatus(state.result).reason;
    } catch (error) {
        dom.cameraState.textContent = 'Error procesando la imagen.';
    }
    setTimeout(detectLoop, 80);
}

async function refresh() {
    try {
        state.authentication = await load('/authentication');
        const active = state.authentication.activeSession;
        dom.enrollmentCard.hidden = !state.authentication.maintenanceMode;
        dom.sessionState.textContent = active
            ? `Sesión activa para ${active.profileId}.`
            : state.authentication.enrolledProfiles === 0
                ? 'No hay ningún rostro registrado.'
                : 'Sin sesión activa.';
        if (state.authentication.maintenanceMode || active) {
            await refreshProfiles();
        }
        updateActions();
    } catch (error) {
        dom.sessionState.textContent = message(error);
    }
}

async function refreshProfiles() {
    const profiles = await load('/profiles');
    dom.profiles.replaceChildren();
    for (const profile of profiles) {
        const item = document.createElement('li');
        const name = document.createElement('span');
        name.textContent = `${profile.displayName} · ${profile.templateCount} captura(s)`;
        item.appendChild(name);
        if (state.authentication.maintenanceMode) {
            const remove = document.createElement('button');
            remove.type = 'button';
            remove.className = 'quiet';
            remove.textContent = 'Eliminar';
            remove.addEventListener('click', () => deleteProfile(profile.id));
            item.appendChild(remove);
        }
        dom.profiles.appendChild(item);
    }
}

async function enroll() {
    const status = faceStatus(state.result);
    if (!status.ready) {
        setFeedback(status.reason, true);
        return;
    }

    state.busy = true;
    updateActions();
    setFeedback('Registrando rostro…');
    try {
        await send('/profiles', {
            displayName: dom.displayName.value.trim(),
            modelVersion: MODEL_VERSION,
            descriptor: Array.from(status.face.embedding),
        });
        dom.displayName.value = '';
        setFeedback('Rostro registrado. Reinicia Presence sin --maintenance para usarlo.');
        await refresh();
    } catch (error) {
        setFeedback(message(error), true);
    } finally {
        state.busy = false;
        updateActions();
    }
}

async function deleteProfile(id) {
    if (!window.confirm('¿Eliminar este perfil biométrico?')) {
        return;
    }
    try {
        await load(`/profiles/${id}`, { method: 'DELETE' });
        await refresh();
    } catch (error) {
        setFeedback(message(error), true);
    }
}

function gestures(result) {
    return (result.gesture || []).map(value => value.gesture);
}

function challengeDetector(type) {
    return result => type === 'VICTORY' && gestures(result).includes('victory');
}

function sleep(milliseconds) {
    return new Promise(resolve => setTimeout(resolve, milliseconds));
}

async function waitForChallenge(challenge) {
    const detected = challengeDetector(challenge.type);
    const expiresAt = Date.parse(challenge.expiresAt);
    while (Date.now() < expiresAt) {
        const remaining = Math.max(0, Math.ceil((expiresAt - Date.now()) / 1000));
        dom.challengeTime.textContent = `${remaining} s`;
        const status = faceStatus(state.result);
        if (status.ready && detected(state.result)) {
            return status.face;
        }
        await sleep(80);
    }
    throw { message: 'El desafío ha caducado. Inténtalo de nuevo.' };
}

async function authenticate() {
    state.busy = true;
    updateActions();
    setFeedback('Preparando desafío…');
    try {
        const challenge = await send('/authentication/challenges', {});
        dom.challenge.hidden = false;
        dom.challengeInstruction.textContent = 'Introduce el código gestual';
        setFeedback('Verificando código gestual…');
        const face = await waitForChallenge(challenge);
        const session = await send(`/authentication/challenges/${challenge.challengeId}/complete`, {
            modelVersion: MODEL_VERSION,
            descriptor: Array.from(face.embedding),
            observedType: challenge.type,
            nonce: challenge.nonce,
            capturedAt: new Date().toISOString(),
        });
        setFeedback(`Sesión ${session.id} abierta correctamente.`);
        await refresh();
    } catch (error) {
        setFeedback(message(error), true);
    } finally {
        dom.challenge.hidden = true;
        state.busy = false;
        updateActions();
    }
}

async function closeSession() {
    try {
        await load(`/sessions/${state.authentication.activeSession.id}`, { method: 'DELETE' });
        setFeedback('Sesión cerrada.');
        await refresh();
    } catch (error) {
        setFeedback(message(error), true);
    }
}

dom.startCamera.addEventListener('click', startCamera);
dom.authenticate.addEventListener('click', authenticate);
dom.enroll.addEventListener('click', enroll);
dom.closeSession.addEventListener('click', closeSession);
dom.displayName.addEventListener('input', updateActions);

const stream = new EventSource('/events');
const eventNames = ['sessionOpened', 'sessionRefreshed', 'sessionExpired', 'sessionClosed', 'gestureDetected'];
eventNames.forEach(name => stream.addEventListener(name, event => {
    dom.events.textContent = `${name}: ${event.data}\n` + dom.events.textContent;
    refresh();
}));
stream.onerror = () => { dom.events.textContent = 'Reconectando…'; };

refresh();
