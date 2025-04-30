// Configuración de Firebase
const firebaseConfig = {
    apiKey: "**apiKeyTetsAIzaSyBmSX-fortets",
    authDomain: "adsv-d87e1.firebaseapp.com",
    databaseURL: "https://adsv-d87e1-default-rtdb.firebaseio.com",
    projectId: "adsv-d87e1",
    storageBucket: "adsv-d87e1.appspot.com",
    messagingSenderId: "123456789012",
    appId: "1:123456789012:web:abcdef1234567890"
};

// Inicializar Firebase
firebase.initializeApp(firebaseConfig);
const database = firebase.database();

// Referencia a la ubicación de los trabajadores activos en la base de datos
const activeWorkersRef = database.ref('active_workers');

// Elementos DOM
const workersContainer = document.getElementById('workers-container');
const loadingElement = document.getElementById('loading');
const noWorkersElement = document.getElementById('no-workers');
const filterButtons = document.querySelectorAll('.filter-btn');
const themeToggle = document.getElementById('theme-toggle');
const themeIcon = document.getElementById('theme-icon');
const appBody = document.getElementById('app-body');

// Almacenar todos los trabajadores para filtrar localmente
let allWorkers = [];

// Verificar si hay preferencia de tema guardada
function initTheme() {
    const savedTheme = localStorage.getItem('theme') || 'light';
    if (savedTheme === 'dark') {
        applyDarkTheme();
    } else {
        applyLightTheme();
    }
}

// Aplicar tema oscuro
function applyDarkTheme() {
    document.documentElement.classList.add('dark-theme');
    appBody.classList.add('dark');
    themeIcon.classList.remove('fa-moon');
    themeIcon.classList.add('fa-sun');
    console.log('Tema oscuro aplicado');
}

// Aplicar tema claro
function applyLightTheme() {
    document.documentElement.classList.remove('dark-theme');
    appBody.classList.remove('dark');
    themeIcon.classList.remove('fa-sun');
    themeIcon.classList.add('fa-moon');
    console.log('Tema claro aplicado');
}

// Cambiar tema claro/oscuro
function toggleTheme() {
    if (document.documentElement.classList.contains('dark-theme')) {
        applyLightTheme();
        localStorage.setItem('theme', 'light');
    } else {
        applyDarkTheme();
        localStorage.setItem('theme', 'dark');
    }

    // Reforzar la aplicación de las clases después de un breve retraso
    setTimeout(() => {
        const currentTheme = localStorage.getItem('theme') || 'light';
        if (currentTheme === 'dark') {
            applyDarkTheme();
        } else {
            applyLightTheme();
        }

        // Refrescar las clases de los botones de filtro
        updateFilterButtonsTheme();
    }, 50);
}

// Event listener para el botón de tema
themeToggle.addEventListener('click', toggleTheme);

// Función para cargar trabajadores desde Firebase
function loadWorkers() {
    // Primero obtenemos la lista de trabajadores activos
    activeWorkersRef.once('value')
        .then(snapshot => {
            loadingElement.classList.add('hidden');

            if (!snapshot.exists()) {
                noWorkersElement.classList.remove('hidden');
                return;
            }

            allWorkers = [];
            let promises = [];

            // Por cada ID en active_workers, buscamos sus datos
            snapshot.forEach(childSnapshot => {
                const workerId = childSnapshot.key;
                console.log("Encontrado ID de trabajador activo:", workerId);

                // Buscar datos del trabajador directamente usando su ID
                // Intentar tres posibles ubicaciones donde podrían estar los datos
                const workerPromise = Promise.all([
                    database.ref(`/${workerId}`).once('value'),
                    database.ref(`/User/Trabajadores/${workerId}`).once('value'),
                    database.ref(`/users/${workerId}`).once('value')
                ])
                .then(results => {
                    // Usar el primer resultado que exista
                    const validSnapshot = results.find(snap => snap.exists());

                    if (validSnapshot) {
                        console.log("Datos del trabajador encontrados:", validSnapshot.ref.path);
                        const worker = validSnapshot.val();
                        worker.id = workerId;
                        // Verificamos si tiene el campo work/typeOfWork para los filtros
                        worker.typeOfWork = worker.work || worker.typeOfWork;
                        allWorkers.push(worker);
                    } else {
                        console.warn("No se encontraron datos para el trabajador:", workerId);
                    }
                    return;
                });

                promises.push(workerPromise);
            });

            // Cuando todos los datos estén cargados, mostramos los trabajadores
            Promise.all(promises).then(() => {
                console.log("Total de trabajadores cargados:", allWorkers.length);
                displayWorkers(allWorkers);
            });
        })
        .catch(error => {
            console.error("Error al cargar trabajadores:", error);
            loadingElement.classList.add('hidden');
            loadingElement.innerHTML = `
                <div class="bg-red-50 dark:bg-red-900 border-l-4 border-red-400 p-4 rounded-md max-w-lg mx-auto">
                    <div class="flex">
                        <div class="flex-shrink-0">
                            <i class="fas fa-exclamation-triangle text-red-400 text-xl"></i>
                        </div>
                        <div class="ml-3">
                            <p class="text-red-700 dark:text-red-200">Error al cargar datos. Por favor, intente nuevamente.</p>
                        </div>
                    </div>
                </div>
            `;
        });
}

// Función para obtener color de categoría
function getCategoryColor(workType) {
    const type = workType.toLowerCase();

    // Mapeo de categorías a colores
    const colorMap = {
        'carpintería': '#A27023', // Marrón
        'carpinteria': '#A27023',
        'ferretería': '#607D8B', // Gris azulado
        'ferreteria': '#607D8B',
        'pintor': '#9C27B0', // Púrpura
        'electricista': '#FFC107', // Amarillo
        'plomería': '#2196F3', // Azul
        'plomeria': '#2196F3',
        'jardinería': '#4CAF50', // Verde
        'jardineria': '#4CAF50',
        'albañilería': '#F44336', // Rojo
        'albañileria': '#F44336'
    };

    return colorMap[type] || '#607D8B'; // Gris por defecto
}

// Función para mostrar trabajadores en el DOM
function displayWorkers(workers) {
    workersContainer.innerHTML = '';

    if (workers.length === 0) {
        noWorkersElement.classList.remove('hidden');
        return;
    }

    noWorkersElement.classList.add('hidden');

    workers.forEach(worker => {
        // Prepara los datos del trabajador
        const nombre = worker.name || worker.firstName || worker.nombre || '';
        const apellido = worker.lastName || worker.lastNombre || '';
        const nombreCompleto = `${nombre} ${apellido}`.trim();
        const email = worker.email || '';
        const telefono = worker.phone || worker.telefono || '';
        const descripcion = worker.description || worker.descripcion || 'Sin descripción';
        const workType = worker.work || worker.typeOfWork || 'No especificado';
        const ubicacion = worker.location || worker.ubicacion || '';
        const rating = worker.rating || null;

        // Crear elemento de la tarjeta
        const card = document.createElement('div');
        card.className = 'worker-card';

        // Obtener color para la categoría
        const categoryColor = getCategoryColor(workType);

        // Construir el contenido HTML
        card.innerHTML = `
            <div class="worker-category" style="background-color: ${categoryColor}">
                <div class="flex justify-between items-center">
                    <span>${workType}</span>
                    ${rating ? `<span class="worker-rating"><i class="fas fa-star mr-1"></i>${rating}</span>` : ''}
                </div>
            </div>
            <div class="worker-info">
                <h3 class="worker-name">${nombreCompleto || 'Sin nombre'}</h3>
                <p class="worker-description">${descripcion}</p>
                <div class="flex items-center">
                    <span class="worker-status">
                        <i class="fas fa-circle text-xs mr-1"></i> Disponible
                    </span>
                </div>
                <div class="worker-contact">
                    ${telefono ? `<p><i class="fas fa-phone"></i> ${telefono}</p>` : ''}
                    ${email ? `<p><i class="fas fa-envelope"></i> ${email}</p>` : ''}
                    ${ubicacion ? `<p><i class="fas fa-map-marker-alt"></i> ${ubicacion}</p>` : ''}
                </div>
            </div>
        `;

        workersContainer.appendChild(card);
    });
}

// Manejar filtrado por tipo de trabajo
filterButtons.forEach(button => {
    button.addEventListener('click', () => {
        // Actualizar estado visual de los botones
        filterButtons.forEach(btn => {
            btn.classList.remove('active', 'bg-primary', 'text-white');
            btn.classList.add('bg-gray-200', 'text-gray-700', 'dark:bg-gray-700', 'dark:text-gray-200');
        });

        button.classList.remove('bg-gray-200', 'text-gray-700', 'dark:bg-gray-700', 'dark:text-gray-200');
        button.classList.add('active', 'bg-primary', 'text-white');

        const filterType = button.getAttribute('data-filter');

        // Filtrar trabajadores
        let filteredWorkers;
        if (filterType === 'todos') {
            filteredWorkers = allWorkers;
        } else {
            filteredWorkers = allWorkers.filter(worker => {
                const workerType = (worker.work || worker.typeOfWork || '').toLowerCase();
                return workerType === filterType.toLowerCase();
            });
        }

        displayWorkers(filteredWorkers);
    });
});

// Función para actualizar las clases de los botones de filtro según el tema
function updateFilterButtonsTheme() {
    const activeButton = document.querySelector('.filter-btn.active');
    const inactiveButtons = document.querySelectorAll('.filter-btn:not(.active)');

    if (document.documentElement.classList.contains('dark-theme')) {
        inactiveButtons.forEach(btn => {
            btn.classList.add('dark:bg-gray-700', 'dark:text-gray-200');
            btn.classList.remove('bg-gray-200', 'text-gray-700');
        });
    } else {
        inactiveButtons.forEach(btn => {
            btn.classList.remove('dark:bg-gray-700', 'dark:text-gray-200');
            btn.classList.add('bg-gray-200', 'text-gray-700');
        });
    }

    // Asegurar que el botón activo siempre tenga las clases correctas
    if (activeButton) {
        activeButton.classList.remove('bg-gray-200', 'text-gray-700', 'dark:bg-gray-700', 'dark:text-gray-200');
        activeButton.classList.add('active', 'bg-primary', 'text-white');
    }
}

// Inicializar tema y cargar trabajadores al iniciar
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    loadWorkers();

    // Agregar un observador de eventos para el botón de tema
    document.getElementById('theme-toggle').addEventListener('click', toggleTheme);

    // Asegurarnos de que el botón de tema esté correctamente inicializado
    setTimeout(updateFilterButtonsTheme, 100);
});