(() => {
    const balanceNet = document.getElementById('economy-net');
    const balanceIncome = document.getElementById('economy-income');
    const balanceExpense = document.getElementById('economy-expense');
    const balancePeriod = document.getElementById('economy-period');
    const breakdownList = document.getElementById('economy-breakdown');
    const breakdownEmpty = document.getElementById('economy-breakdown-empty');
    const movementList = document.getElementById('economy-movements');
    const movementsEmpty = document.getElementById('economy-movements-empty');
    const goalsSection = document.getElementById('economy-goals-section');
    const goalList = document.getElementById('economy-goals');
    const summarySpent = document.getElementById('today-spent');
    const summaryNet = document.getElementById('today-balance');
    let started = false;

    const MONTHS = [
        'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
        'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre',
    ];

    async function read(path) {
        const response = await fetch(path);
        if (!response.ok) throw new Error('No se pudo leer ' + path);
        return response.json();
    }

    function euros(amount) {
        return Number(amount).toLocaleString('es-ES', {
            style: 'currency', currency: 'EUR', minimumFractionDigits: 2,
        });
    }

    function periodText(balance) {
        const from = new Date(balance.from + 'T00:00:00');
        const to = new Date(balance.to + 'T00:00:00');
        if (from.getMonth() === to.getMonth() && from.getFullYear() === to.getFullYear()) {
            return MONTHS[from.getMonth()] + ' ' + from.getFullYear();
        }
        return balance.from + ' — ' + balance.to;
    }

    function dayText(day) {
        const date = new Date(day + 'T00:00:00');
        return date.getDate() + ' ' + MONTHS[date.getMonth()].slice(0, 3);
    }

    function renderBalance(balance) {
        balanceNet.textContent = euros(balance.net);
        balanceNet.classList.toggle('negative', Number(balance.net) < 0);
        balanceIncome.textContent = euros(balance.income);
        balanceExpense.textContent = euros(balance.expense);
        balancePeriod.textContent = periodText(balance);
    }

    function breakdownRow(spend, budget) {
        const item = document.createElement('li');
        item.className = 'economy-category';

        const head = document.createElement('div');
        head.className = 'economy-category-head';

        const name = document.createElement('span');
        name.className = 'economy-category-name';
        name.textContent = spend.icon + ' ' + spend.label;

        const total = document.createElement('span');
        total.className = 'economy-category-total';
        total.textContent = budget
            ? euros(spend.total) + ' / ' + euros(budget.limit)
            : euros(spend.total);

        head.append(name, total);

        const bar = document.createElement('div');
        bar.className = 'economy-bar';
        if (budget) {
            bar.dataset.status = budget.status;
        }
        const fill = document.createElement('span');
        fill.style.width = Math.min(100, budget
            ? 100 * Number(budget.spent) / Number(budget.limit)
            : Number(spend.percentage)) + '%';
        bar.append(fill);

        item.append(head, bar);

        if (budget && budget.status !== 'WITHIN') {
            const pace = document.createElement('p');
            pace.className = 'economy-pace';
            pace.textContent = budget.status === 'EXCEEDED'
                ? 'Ya superado'
                : 'A este ritmo, ' + euros(budget.projected);
            item.append(pace);
        }

        return item;
    }

    function movementRow(movement) {
        const item = document.createElement('li');
        item.className = 'economy-movement';

        const day = document.createElement('span');
        day.className = 'economy-movement-day';
        day.textContent = dayText(movement.occurredOn);

        const what = document.createElement('span');
        what.className = 'economy-movement-what';
        what.textContent = movement.note || movement.categoryLabel;

        const category = document.createElement('span');
        category.className = 'economy-movement-category';
        category.textContent = movement.categoryIcon;

        const amount = document.createElement('span');
        amount.className = movement.kind === 'INCOME'
            ? 'economy-movement-amount income'
            : 'economy-movement-amount';
        amount.textContent = (movement.kind === 'INCOME' ? '+' : '−') + euros(movement.amount);

        item.append(day, category, what, amount);
        return item;
    }

    function replace(list, rows) {
        list.replaceChildren(...rows);
    }

    async function refreshBalance() {
        renderBalance(await read('/economy/balance'));
    }

    async function refreshBreakdown() {
        const [spending, budgets] = await Promise.all([
            read('/economy/breakdown'),
            read('/economy/budgets'),
        ]);
        const byCategory = new Map(budgets.map(budget => [budget.category, budget]));

        replace(breakdownList, spending.map(spend => breakdownRow(spend, byCategory.get(spend.category))));
        breakdownEmpty.hidden = spending.length > 0;
    }

    function goalRow(goal) {
        const item = document.createElement('li');
        item.className = goal.reachable ? 'economy-goal reachable' : 'economy-goal';

        const head = document.createElement('div');
        head.className = 'economy-goal-head';

        const name = document.createElement('span');
        name.className = 'economy-goal-name';
        name.textContent = goal.name;

        const target = document.createElement('span');
        target.className = 'economy-goal-target';
        target.textContent = euros(goal.target) + ' · ' + monthText(goal.deadline);

        head.append(name, target);

        const forecast = document.createElement('p');
        forecast.className = 'economy-goal-forecast';
        forecast.textContent = goal.reachable
            ? 'A este ritmo llegas: ' + euros(goal.projected)
            : 'Necesitas ' + euros(goal.requiredMonthly) + ' al mes; vas a ' + euros(goal.monthlySaving);

        item.append(head, forecast);
        return item;
    }

    function monthText(day) {
        const date = new Date(day + 'T00:00:00');
        return MONTHS[date.getMonth()] + ' ' + date.getFullYear();
    }

    async function refreshGoals() {
        const goals = await read('/economy/goals');
        replace(goalList, goals.map(goalRow));
        goalsSection.hidden = goals.length === 0;
    }

    async function refreshMovements() {
        const movements = await read('/economy/movements?limit=12');
        replace(movementList, movements.map(movementRow));
        movementsEmpty.hidden = movements.length > 0;
    }

    async function refresh() {
        await Promise.all([refreshBalance(), refreshBreakdown(), refreshMovements(), refreshGoals()]);
    }

    async function refreshSummary() {
        const balance = await read('/economy/balance');
        if (summarySpent) summarySpent.textContent = euros(balance.expense);
        if (summaryNet) {
            summaryNet.textContent = euros(balance.net);
            summaryNet.classList.toggle('negative', Number(balance.net) < 0);
        }
    }

    function guard(work) {
        return work().catch(() => {});
    }

    function listen() {
        if (started) return;
        started = true;

        const events = new EventSource('/events/economy');
        const reload = () => {
            guard(refresh);
            guard(refreshSummary);
        };
        ['movementRecorded', 'movementCorrected', 'movementRecategorized', 'movementDeleted',
            'budgetDefined', 'budgetLimitChanged', 'budgetRemoved',
            'savingsGoalSet', 'savingsGoalChanged', 'savingsGoalAbandoned']
            .forEach(name => events.addEventListener(name, reload));
    }

    window.AtlasEconomy = {
        activate() {
            guard(refresh);
            listen();
        },
        summary() {
            guard(refreshSummary);
            listen();
        },
    };
})();
