/* Same-origin session API. No credentials, accounts or tokens in browser storage. */
(function(root) {
  'use strict';
  function createClient(fetcher) {
    class AuthError extends Error {
      constructor(status, message) { super(message); this.status = status; }
    }
    async function request(path, options = {}) {
      let response;
      try {
        response = await fetcher('/api/auth/' + path, {
          ...options, credentials: 'same-origin', cache: 'no-store', redirect: 'error',
          signal: AbortSignal.timeout(15000),
          headers: {Accept: 'application/json', ...options.headers}
        });
      } catch { throw new AuthError(0, 'Could not confirm the result. Check your connection and try again.'); }
      if (!response.ok) {
        const messages = {
          400: 'Check your details and try again.',
          401: 'Invalid email or password.',
          403: 'Your session changed. Please try again.',
          409: 'An account with this email already exists. Please sign in.'
        };
        throw new AuthError(response.status, messages[response.status] || 'Account services are unavailable right now.');
      }
      if (response.status === 204) return null;
      if (!(response.headers.get('content-type') || '').includes('application/json')) {
        throw new AuthError(503, 'Account services are unavailable on this preview.');
      }
      try { return await response.json(); }
      catch { throw new AuthError(503, 'Account services returned an unreadable response.'); }
    }
    async function csrf() {
      const value = await request('csrf');
      if (!value || value.headerName !== 'X-CSRF-TOKEN' || typeof value.token !== 'string' || !value.token) {
        throw new AuthError(503, 'Account services are unavailable on this preview.');
      }
      return value;
    }
    async function mutate(path, body, type) {
      // Get the token for the current session before each write. Never retry a POST automatically.
      const token = await csrf();
      return request(path, {method: 'POST', body,
        headers: {[token.headerName]: token.token, ...(type ? {'Content-Type': type} : {})}});
    }
    async function me() {
      try { return await request('me'); }
      catch (error) { if (error.status === 401) return null; throw error; }
    }
    return {
      csrf, me,
      register: data => mutate('register', JSON.stringify({fullName:data.fullName,email:data.email,password:data.password}), 'application/json'),
      login: async (email, password) => {
        await mutate('login', new URLSearchParams({email,password}).toString(), 'application/x-www-form-urlencoded');
        const account = await me();
        if (!account) throw new AuthError(401, 'Sign-in could not be confirmed. Please try again.');
        return account;
      },
      logout: () => mutate('logout')
    };
  }
  root.HotelVistaAuth = {createClient};
})(typeof window !== 'undefined' ? window : globalThis);
