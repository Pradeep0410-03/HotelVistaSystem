(() => {
  'use strict';
  const $ = id => document.getElementById(id);
  const params = new URLSearchParams(location.search);
  const now = new Date();
  const today = [now.getFullYear(), String(now.getMonth()+1).padStart(2,'0'), String(now.getDate()).padStart(2,'0')].join('-');
  const trip = window.HotelVistaIntent.parse(params, window.HOTEL_VISTA.properties, today);
  const register = document.body.dataset.account === 'register';
  if (trip) {
    const property = window.HOTEL_VISTA.properties.find(p => p.id === trip.property);
    const estimate = window.HotelVistaCatalogue.estimate(property.price, trip.checkin, trip.checkout, trip.rooms);
    $('trip-summary').hidden = false;
    $('trip-property').textContent = property.name;
    $('trip-dates').textContent = trip.checkin + ' → ' + trip.checkout + ' · ' + estimate.nights + (estimate.nights === 1 ? ' night' : ' nights');
    $('trip-guests').textContent = trip.guests + (trip.guests === 1 ? ' guest' : ' guests') + ' · ' + trip.rooms + (trip.rooms === 1 ? ' room' : ' rooms');
    $('trip-price').textContent = new Intl.NumberFormat('en-IN', {style:'currency',currency:'INR',maximumFractionDigits:0}).format(estimate.subtotal) + ' estimated subtotal';
    $('switch-account').href = window.HotelVistaIntent.link(register ? 'login.html' : 'register.html', trip);
    const back = window.HotelVistaIntent.link('index.html', trip) + '&destination=' + encodeURIComponent(property.city) + '#stays';
    $('back-to-stays').href = back;
    $('edit-trip').href = back;
  } else if (params.has('property')) {
    $('account-status').hidden = false;
    $('account-status').textContent = 'Your stay selection is invalid or its dates have passed. Go back to stays to choose again.';
  }
  $('toggle-password').addEventListener('click', () => {
    const show = $('password').type === 'password';
    $('password').type = show ? 'text' : 'password';
    $('toggle-password').textContent = show ? 'Hide' : 'Show';
    $('toggle-password').setAttribute('aria-label', show ? 'Hide password' : 'Show password');
    $('toggle-password').setAttribute('aria-pressed', String(show));
  });
  function validateConfirmation() {
    if (!register) return;
    const mismatch = $('confirm-password').value !== $('password').value;
    $('confirm-password').setCustomValidity(mismatch ? 'Passwords must match.' : '');
    $('confirm-password').setAttribute('aria-invalid', String(mismatch && !!$('confirm-password').value));
    $('password-error').hidden = !mismatch || !$('confirm-password').value;
  }
  if (register) {
    ['password', 'confirm-password'].forEach(id => $(id).addEventListener('input', validateConfirmation));
    $('full-name').addEventListener('input', () => $('full-name').setCustomValidity($('full-name').value.trim() ? '' : 'Enter your name.'));
  }
  $('account-form').addEventListener('submit', event => {
    event.preventDefault();
    validateConfirmation();
    if (!$('account-form').reportValidity()) return;
    // Stop at the preview boundary. Never simulate an authenticated session.
    $('password').value = '';
    $('password').type = 'password';
    $('toggle-password').textContent = 'Show';
    $('toggle-password').setAttribute('aria-label', 'Show password');
    $('toggle-password').setAttribute('aria-pressed', 'false');
    if (register) { $('confirm-password').value = ''; validateConfirmation(); }
    $('account-status').hidden = false;
    $('account-status').textContent = 'Form checked. ' + (register ? 'No account was created.' : 'You have not been signed in.') + ' Account services are coming in the backend phase.' + (trip ? ' Your stay selection is still here; no rooms are reserved.' : '');
    $('account-status').focus();
  });
  // With JavaScript unavailable the button stays disabled; inputs have no submission names.
  $('account-submit').disabled = false;
})();
