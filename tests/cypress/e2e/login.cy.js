describe('Módulo de Autenticación - HostFlow', () => {
  it('Debería permitir iniciar sesión con credenciales válidas', () => {
    // 1. Visitar la página de login
    cy.visit('http://localhost:5173/auth/login');

    // 2. Ingresar el correo electrónico utilizando el placeholder o tipo de input
    cy.get('input[type="email"]').type('ms.perezgomez@gmail.com'); // O tu correo de prueba registrado

    // 3. Ingresar la contraseña
    cy.get('input[type="password"]').type('Test123456*');

    // 4. Hacer clic en el botón "Ingresar"
    cy.get('button').contains('Ingresar').click();

    // 5. Verificar que la URL cambie al dashboard o panel principal tras el éxito
    cy.url().should('include', '/dashboard'); 
  });
});