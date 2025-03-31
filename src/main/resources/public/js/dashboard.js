document.addEventListener('DOMContentLoaded', function() {
    // WebSocket connection
    let socket;
    const baseUrl = window.location.host;
    connectWebSocket();

    // Counters for dashboard stats
    let stats = {
        totalAlerts: 0,
        accountAlerts: 0,
        counterpartyAlerts: 0,
        highValueAlerts: 0
    };

    // DOM elements
    const alertsTable = document.getElementById('alerts-body');
    const noAlertsMessage = document.getElementById('no-alerts-message');
    const clearAlertsBtn = document.getElementById('clear-alerts');
    const searchInput = document.getElementById('search-input');
    const searchBtn = document.getElementById('search-btn');

    // Modal elements
    const modal = document.getElementById('alert-modal');
    const closeModal = document.getElementsByClassName('close')[0];
    const modalBody = document.getElementById('modal-body');
    const investigateBtn = document.getElementById('investigate-btn');
    const dismissBtn = document.getElementById('dismiss-btn');

    // Connect to WebSocket
    function connectWebSocket() {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        socket = new WebSocket(`${protocol}//${baseUrl}/ws/alerts`);

        socket.onopen = function() {
            console.log('WebSocket connected');
            updateConnectionStatus(true);
        };

        socket.onmessage = function(event) {
            console.log('Message received:', event.data);
            processAlert(event.data);
        };

        socket.onclose = function() {
            console.log('WebSocket disconnected');
            updateConnectionStatus(false);

            // Attempt to reconnect after a delay
            setTimeout(connectWebSocket, 5000);
        };

        socket.onerror = function(error) {
            console.error('WebSocket error:', error);
            updateConnectionStatus(false);
        };
    }

    // Update connection status display
    function updateConnectionStatus(connected) {
        const statusIcon = document.getElementById('status-icon');
        const statusText = document.getElementById('status-text');

        if (connected) {
            statusIcon.className = 'status-connected';
            statusIcon.innerHTML = '<i class="fas fa-plug"></i>';
            statusText.textContent = 'Connected';
        } else {
            statusIcon.className = 'status-disconnected';
            statusIcon.innerHTML = '<i class="fas fa-plug"></i>';
            statusText.textContent = 'Disconnected';
        }
    }

    // Process and display a new alert
    function processAlert(data) {
        try {
            const transaction = JSON.parse(data);

            // Update statistics
            stats.totalAlerts++;
            document.getElementById('total-alerts').textContent = stats.totalAlerts;

            // Check for account blacklist
            if (transaction.blacklistReason && transaction.blacklistReason.includes('Account:')) {
                stats.accountAlerts++;
                document.getElementById('account-alerts').textContent = stats.accountAlerts;
            }

            // Check for counterparty blacklist
            if (transaction.blacklistReason && transaction.blacklistReason.includes('Counterparty:')) {
                stats.counterpartyAlerts++;
                document.getElementById('counterparty-alerts').textContent = stats.counterpartyAlerts;
            }

            // Check for high value transactions (over 5000)
            if (transaction.amount > 5000) {
                stats.highValueAlerts++;
                document.getElementById('high-value-alerts').textContent = stats.highValueAlerts;
            }

            // Add alert to table
            addAlertToTable(transaction);

            // Hide no alerts message if there are alerts
            noAlertsMessage.style.display = 'none';

        } catch (error) {
            console.error('Error processing alert:', error);
        }
    }

    // Add alert to the table
    function addAlertToTable(transaction) {
        const row = document.createElement('tr');
        row.setAttribute('data-transaction', JSON.stringify(transaction));

        // Format time
        const timestamp = new Date(transaction.timestamp);
        const formattedTime = timestamp.toLocaleTimeString();

        // Format amount with currency
        const formattedAmount = new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: transaction.currency || 'USD'
        }).format(transaction.amount);

        // Check if it's a high value transaction
        const amountClass = transaction.amount > 5000 ? 'alert-high' : '';

        row.innerHTML = `
            <td>${formattedTime}</td>
            <td>${transaction.transactionId}</td>
            <td>${transaction.accountId}</td>
            <td>${transaction.counterpartyId}</td>
            <td class="${amountClass}">${formattedAmount}</td>
            <td><span class="alert-reason">${transaction.blacklistReason || 'Unknown'}</span></td>
            <td>
                <button class="action-btn view-btn" title="View Details"><i class="fas fa-eye"></i></button>
                <button class="action-btn dismiss-btn" title="Dismiss"><i class="fas fa-times"></i></button>
            </td>
        `;

        // Add to the top of the table
        if (alertsTable.firstChild) {
            alertsTable.insertBefore(row, alertsTable.firstChild);
        } else {
            alertsTable.appendChild(row);
        }

        // Add event listeners to the buttons
        const viewBtn = row.querySelector('.view-btn');
        const dismissBtn = row.querySelector('.dismiss-btn');

        viewBtn.addEventListener('click', function() {
            showTransactionDetails(transaction);
        });

        dismissBtn.addEventListener('click', function() {
            row.remove();
            checkEmptyTable();
        });
    }

    // Show transaction details in modal
    function showTransactionDetails(transaction) {
        // Format time
        const timestamp = new Date(transaction.timestamp);
        const formattedTime = timestamp.toLocaleString();

        // Format amount with currency
        const formattedAmount = new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: transaction.currency || 'USD'
        }).format(transaction.amount);

        // Create details content
        const detailsHtml = `
            <dl>
                <dt>Transaction ID:</dt>
                <dd>${transaction.transactionId}</dd>
                
                <dt>Time:</dt>
                <dd>${formattedTime}</dd>
                
                <dt>Account ID:</dt>
                <dd>${transaction.accountId}</dd>
                
                <dt>Counterparty ID:</dt>
                <dd>${transaction.counterpartyId}</dd>
                
                <dt>Amount:</dt>
                <dd class="${transaction.amount > 5000 ? 'alert-high' : ''}">${formattedAmount}</dd>
                
                <dt>Currency:</dt>
                <dd>${transaction.currency}</dd>
                
                <dt>Blacklist Reason:</dt>
                <dd><span class="alert-reason">${transaction.blacklistReason || 'Unknown'}</span></dd>
            </dl>
        `;

        modalBody.innerHTML = detailsHtml;
        modal.style.display = 'block';

        // Store transaction data on buttons for reference
        investigateBtn.setAttribute('data-transaction', JSON.stringify(transaction));
        dismissBtn.setAttribute('data-transaction', JSON.stringify(transaction));
    }

    // Check if table is empty and show message if needed
    function checkEmptyTable() {
        if (alertsTable.children.length === 0) {
            noAlertsMessage.style.display = 'block';
        } else {
            noAlertsMessage.style.display = 'none';
        }
    }

    // Clear all alerts
    clearAlertsBtn.addEventListener('click', function() {
        alertsTable.innerHTML = '';
        noAlertsMessage.style.display = 'block';
    });

    // Search functionality
    searchBtn.addEventListener('click', performSearch);
    searchInput.addEventListener('keyup', function(event) {
        if (event.key === 'Enter') {
            performSearch();
        }
    });

    function performSearch() {
        const searchTerm = searchInput.value.toLowerCase();
        const rows = alertsTable.getElementsByTagName('tr');

        for (let i = 0; i < rows.length; i++) {
            const rowData = rows[i].textContent.toLowerCase();
            if (rowData.includes(searchTerm)) {
                rows[i].style.display = '';
            } else {
                rows[i].style.display = 'none';
            }
        }
    }

    // Modal close button
    closeModal.addEventListener('click', function() {
        modal.style.display = 'none';
    });

    // Close modal when clicking outside
    window.addEventListener('click', function(event) {
        if (event.target === modal) {
            modal.style.display = 'none';
        }
    });

    // Investigate button (demo functionality)
    investigateBtn.addEventListener('click', function() {
        alert('Alert marked for investigation. In a real system, this would create a case in your case management system.');
        modal.style.display = 'none';
    });

    // Dismiss button in modal
    dismissBtn.addEventListener('click', function() {
        const transaction = JSON.parse(this.getAttribute('data-transaction'));

        // Find and remove this transaction from table
        const rows = alertsTable.getElementsByTagName('tr');
        for (let i = 0; i < rows.length; i++) {
            const rowTransaction = JSON.parse(rows[i].getAttribute('data-transaction'));
            if (rowTransaction.transactionId === transaction.transactionId) {
                rows[i].remove();
                break;
            }
        }

        modal.style.display = 'none';
        checkEmptyTable();
    });

    // Initial check for empty table
    checkEmptyTable();
});