# Druk Connect Resend setup — verified domain

bhutanesemarketplace.au is verified in Resend. GoDaddy saved the DKIM TXT record and both sending CNAME records, and authoritative DNS queries confirmed their exact values. Existing website, Microsoft 365 MX/SPF/DKIM, and DMARC records were retained. Resend receiving is disabled.

## Current sender

noreply@bhutanesemarketplace.au

The private, Git-ignored .env.properties file selects this sender and Resend for local startup. Restart any running local backend from the backend directory to load the changed sender. Do not commit .env.properties.

Render service druk-connect-backend-1 has MAIL_PROVIDER=resend, RESEND_API_KEY, and the new MAIL_FROM saved. Save only was used: the deployed backend still needs the local Resend code changes committed, pushed, and deployed before those settings take effect.

## Verification

Resend shows STATUS Verified and Domain verified. A live email sent through the backend ResendMailSender from noreply@bhutanesemarketplace.au to wivolo6384@nbasweeps.com was accepted successfully. Inbox delivery has not been independently confirmed. The old onboarding sender's recipient restriction no longer applies when sending from this verified domain; normal provider quotas and delivery policies still apply.

The existing test suite passed 57 tests when the HTTPS transport was implemented. Normal application-context startup with the local Resend configuration was also verified. The current change is the sender configuration.

## Configuration

MAIL_PROVIDER=resend
MAIL_FROM=noreply@bhutanesemarketplace.au
RESEND_API_KEY=<stored privately>

SMTP credentials are not used by Resend. Local startup imports .env.properties from the backend working directory. Production uses Render environment variables.
