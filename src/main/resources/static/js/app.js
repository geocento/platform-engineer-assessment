(function () {
    'use strict';

    const API_BASE = '';
    const HEALTH_URL = API_BASE + '/api/health';
    const SEARCH_URL = API_BASE + '/api/products/search';
    const KEYCLOAK_URL = 'http://localhost:8180';
    const KEYCLOAK_REALM = 'geocento';
    const KEYCLOAK_CLIENT_ID = 'java-full-stack-assessment';

    let keycloak = null;
    let basicCredentials = null;
    let map = null;
    let drawnLayer = null;
    let drawnItems = null;
    let productLayers = [];
    let drawControl = null;

    const loginScreen = document.getElementById('login-screen');
    const appScreen = document.getElementById('app-screen');
    const loginBtn = document.getElementById('login-btn');
    const loginStatus = document.getElementById('login-status');
    const loginError = document.getElementById('login-error');
    const logoutBtn = document.getElementById('logout-btn');
    const userNameEl = document.getElementById('user-name');
    const aoiJson = document.getElementById('aoi-json');
    const searchBtn = document.getElementById('search-btn');
    const clearBtn = document.getElementById('clear-btn');
    const productList = document.getElementById('product-list');

    function authHeader() {
        if (keycloak && keycloak.authenticated && keycloak.token) {
            return { 'Authorization': 'Bearer ' + keycloak.token };
        }
        if (basicCredentials && basicCredentials.username) {
            return { 'Authorization': 'Basic ' + btoa(basicCredentials.username + ':' + basicCredentials.password) };
        }
        return {};
    }

    function showLogin() {
        loginScreen.classList.remove('hidden');
        appScreen.classList.add('hidden');
        basicCredentials = null;
    }

    function showApp() {
        loginScreen.classList.add('hidden');
        appScreen.classList.remove('hidden');
        if (keycloak && keycloak.tokenParsed && keycloak.tokenParsed.preferred_username) {
            userNameEl.textContent = keycloak.tokenParsed.preferred_username;
        } else if (basicCredentials && basicCredentials.username) {
            userNameEl.textContent = basicCredentials.username;
        }
        initMap();
    }

    function setLoginError(msg) {
        loginError.textContent = msg || '';
    }

    function setLoginStatus(msg) {
        if (!loginStatus) return;
        loginStatus.textContent = msg || '';
    }

    async function ensureToken() {
        if (basicCredentials && basicCredentials.username) return true;
        if (!keycloak || !keycloak.authenticated) return false;
        try {
            await keycloak.updateToken(30);
            return !!keycloak.token;
        } catch (e) {
            return false;
        }
    }

    async function checkAuth() {
        if (basicCredentials && basicCredentials.username) {
            const r = await fetch(HEALTH_URL, { headers: authHeader() });
            return r.ok;
        }
        if (!(await ensureToken())) return false;
        const r = await fetch(HEALTH_URL, { headers: authHeader() });
        return r.ok;
    }

    function showBasicLoginFallback() {
        setLoginStatus('Keycloak not running. Sign in with username/password below.');
        setLoginError('');
        const kcDiv = document.getElementById('keycloak-login');
        const basicDiv = document.getElementById('basic-login');
        if (kcDiv) kcDiv.classList.add('hidden');
        if (basicDiv) basicDiv.classList.remove('hidden');
    }

    function initKeycloak() {
        keycloak = new Keycloak({
            url: KEYCLOAK_URL,
            realm: KEYCLOAK_REALM,
            clientId: KEYCLOAK_CLIENT_ID
        });
        var fallbackTimer = setTimeout(showBasicLoginFallback, 4000);
        keycloak.init({ onLoad: 'check-sso', checkLoginIframe: false })
            .then(function (authenticated) {
                clearTimeout(fallbackTimer);
                if (authenticated) {
                    setLoginStatus('');
                    showApp();
                } else {
                    setLoginStatus('Not signed in.');
                    const kcDiv = document.getElementById('keycloak-login');
                    if (kcDiv) kcDiv.classList.remove('hidden');
                    if (loginBtn) loginBtn.onclick = function () { keycloak.login(); };
                }
            })
            .catch(function () {
                clearTimeout(fallbackTimer);
                showBasicLoginFallback();
            });
    }

    if (loginBtn) {
        loginBtn.addEventListener('click', function () {
            if (keycloak) keycloak.login();
        });
    }
    const basicForm = document.getElementById('basic-login-form');
    if (basicForm) {
        basicForm.addEventListener('submit', async function (e) {
            e.preventDefault();
            basicCredentials = {
                username: document.getElementById('basic-username').value.trim(),
                password: document.getElementById('basic-password').value
            };
            setLoginError('');
            if (await checkAuth()) {
                setLoginStatus('');
                showApp();
            } else {
                setLoginError('Wrong username or password. Use user / password.');
            }
        });
    }
    if (logoutBtn) {
        logoutBtn.addEventListener('click', function () {
            if (keycloak) keycloak.logout();
            else showLogin();
        });
    }

    function initMap() {
        if (map) return;
        map = L.map('map').setView([48, 2], 5);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; OpenStreetMap'
        }).addTo(map);

        drawnItems = new L.FeatureGroup();
        map.addLayer(drawnItems);

        drawControl = new L.Control.Draw({
            draw: {
                polygon: {
                    shapeOptions: { color: '#82b1ff' },
                    allowIntersection: false
                },
                polyline: false,
                circle: false,
                rectangle: false,
                marker: false,
                circlemarker: false
            },
            edit: {
                featureGroup: drawnItems
            }
        });
        map.addControl(drawControl);

        map.on(L.Draw.Event.CREATED, function (e) {
            const layer = e.layer;
            drawnItems.clearLayers();
            drawnItems.addLayer(layer);
            drawnLayer = layer;
            const geojson = layer.toGeoJSON();
            if (geojson.geometry && geojson.geometry.type === 'Polygon') {
                aoiJson.value = JSON.stringify(geojson.geometry, null, 2);
            }
        });
    }

    function getAoiGeometry() {
        const raw = aoiJson.value.trim();
        if (!raw) return null;
        try {
            const parsed = JSON.parse(raw);
            const poly = (parsed.type === 'Feature' && parsed.geometry) ? parsed.geometry : parsed;
            if (poly.type !== 'Polygon' || !Array.isArray(poly.coordinates) || poly.coordinates.length === 0) {
                return null;
            }
            const ring = poly.coordinates[0];
            if (!Array.isArray(ring) || ring.length < 4) {
                return null;
            }
            const valid = ring.every(function (p) {
                return Array.isArray(p) && p.length >= 2 && typeof p[0] === 'number' && typeof p[1] === 'number';
            });
            return valid ? poly : null;
        } catch (_) {
            return null;
        }
    }

    const EXAMPLE_AOI = '{"type":"Polygon","coordinates":[[[-0.5,51],[0.5,51],[0.5,52],[-0.5,52],[-0.5,51]]]}';

    function showProductError(msg, withRetry) {
        productList.innerHTML = '';
        const p = document.createElement('p');
        p.className = 'text-danger small';
        p.textContent = msg;
        productList.appendChild(p);
        if (withRetry) {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'btn btn-primary btn-sm';
            btn.textContent = 'Sign in again';
            btn.onclick = function () { if (keycloak) keycloak.login(); else showLogin(); };
            productList.appendChild(btn);
        }
    }

    async function searchProducts() {
        const aoi = getAoiGeometry();
        if (!aoi) {
            productList.innerHTML = '';
            const p = document.createElement('p');
            p.className = 'text-danger small';
            p.textContent = 'Need a valid GeoJSON Polygon. Draw on the map or paste one.';
            productList.appendChild(p);
            return;
        }
        if (!(await ensureToken())) {
            showProductError('Session expired.', true);
            return;
        }

        searchBtn.disabled = true;
        productList.innerHTML = '';
        const searching = document.createElement('p');
        searching.className = 'text-muted small';
        searching.textContent = 'Searching…';
        productList.appendChild(searching);
        clearProductLayers();

        try {
            const r = await fetch(SEARCH_URL, {
                method: 'POST',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json',
                    ...authHeader()
                },
                body: JSON.stringify(aoi),
                redirect: 'manual'
            });
            let text = await r.text();
            if (typeof text === 'string') {
                text = text.replace(/^\uFEFF/, '').trim();
            }
            if (!r.ok) {
                if (r.status === 401 || r.status === 302) {
                    if (keycloak) keycloak.login(); else showLogin();
                    return;
                }
                let errMsg = 'Search failed: ' + r.status;
                if (text && (text.startsWith('{') || text.startsWith('['))) {
                    try { const j = JSON.parse(text); errMsg = j.error || errMsg; } catch (_) {}
                } else if (text && text.substring(0, 100).indexOf('<') !== -1) {
                    errMsg = 'Server returned HTML (login page?). Try signing in again.';
                }
                throw new Error(errMsg);
            }
            let data;
            if (text && (text.trim().startsWith('{') || text.trim().startsWith('['))) {
                try {
                    data = JSON.parse(text);
                } catch (e) {
                    showProductError('Invalid response from server.');
                    return;
                }
            } else {
                if (text && text.substring(0, 100).indexOf('<') !== -1) {
                    showProductError('Got login page instead of data.', true);
                    return;
                }
                showProductError(!text ? 'Empty response.' : 'Unexpected response.');
                return;
            }
            if (!data || typeof data.features === 'undefined') {
                showProductError('Invalid response format.');
                return;
            }
            renderProducts(data);
            drawProductFootprints(data);
        } catch (err) {
            showProductError(err.message || 'Request failed');
        } finally {
            searchBtn.disabled = false;
        }
    }

    function clearProductLayers() {
        productLayers.forEach(function (layer) {
            if (map && map.hasLayer(layer)) map.removeLayer(layer);
        });
        productLayers = [];
    }

    function drawProductFootprints(featureCollection) {
        if (!map || !featureCollection.features) return;
        clearProductLayers();
        featureCollection.features.forEach(function (f) {
            if (!f.geometry || f.geometry.type !== 'Polygon') return;
            const layer = L.geoJSON(f, {
                style: { color: '#82b1ff', weight: 2, fillOpacity: 0.2 }
            });
            layer.addTo(map);
            productLayers.push(layer);
        });
    }

    function formatDate(iso) {
        if (!iso) return '—';
        const d = new Date(iso);
        return isNaN(d.getTime()) ? iso : d.toLocaleString();
    }

    function productCard(feature) {
        const p = feature.properties || {};
        const card = document.createElement('div');
        card.className = 'card product-card';
        card.dataset.id = p.id || '';
        const body = document.createElement('div');
        body.className = 'card-body';

        const h3 = document.createElement('h3');
        h3.className = 'card-title';
        h3.textContent = p.name || '—';
        body.appendChild(h3);

        const meta1 = document.createElement('p');
        meta1.className = 'meta text-muted small';
        meta1.textContent = (p.provider || '') + ' · ' + (p.resolution || '');
        body.appendChild(meta1);

        const meta2 = document.createElement('p');
        meta2.className = 'meta text-muted small';
        meta2.textContent = 'Acquired: ' + formatDate(p.acquisitionDate);
        body.appendChild(meta2);

        if (p.metadata) {
            const meta3 = document.createElement('p');
            meta3.className = 'meta text-muted small';
            meta3.textContent = 'Sensor: ' + (p.metadata.sensor || '') + ', clouds: ' + (p.metadata.cloudCoverage ?? '') + '%';
            body.appendChild(meta3);
        }

        const img = document.createElement('img');
        img.className = 'thumb';
        img.alt = 'Product thumbnail';
        const thumbUrl = (p.thumbnailUrl && String(p.thumbnailUrl).startsWith('/'))
            ? (API_BASE + p.thumbnailUrl) : '/thumbnails/placeholder.svg';
        img.src = thumbUrl;
        img.onerror = function () { img.src = '/thumbnails/placeholder.svg'; img.alt = 'Placeholder'; };
        body.appendChild(img);

        card.appendChild(body);
        return card;
    }

    function renderProducts(featureCollection) {
        const features = featureCollection.features || [];
        productList.innerHTML = '';
        if (features.length === 0) {
            const p = document.createElement('p');
            p.className = 'text-muted small';
            p.textContent = 'No products in this area. Try a different region.';
            productList.appendChild(p);
            return;
        }
        features.forEach(function (f) { productList.appendChild(productCard(f)); });
    }

    const pasteExample = document.getElementById('paste-example');
    if (pasteExample) {
        pasteExample.addEventListener('click', function (e) {
            e.preventDefault();
            aoiJson.value = EXAMPLE_AOI;
        });
    }

    searchBtn.addEventListener('click', searchProducts);
    clearBtn.addEventListener('click', function () {
        aoiJson.value = '';
        clearProductLayers();
        if (drawnLayer && map) {
            map.removeLayer(drawnLayer);
            drawnLayer = null;
        }
        if (drawnItems && drawnItems.clearLayers) drawnItems.clearLayers();
        productList.innerHTML = '';
    });

    if (typeof Keycloak !== 'undefined') {
        initKeycloak();
    } else {
        setLoginStatus('');
        setLoginError('Keycloak adapter failed to load.');
    }
})();
