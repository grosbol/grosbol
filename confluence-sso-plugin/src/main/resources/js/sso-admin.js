// SSO Plugin Admin — minor UX enhancements
(function () {
    'use strict';

    // Copy SP metadata URL to clipboard
    var metaLink = document.querySelector('a[href*="/sso/saml/metadata"]');
    if (metaLink) {
        var btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'aui-button aui-button-subtle';
        btn.textContent = 'Copy URL';
        btn.style.marginLeft = '8px';
        btn.addEventListener('click', function () {
            navigator.clipboard.writeText(metaLink.href).then(function () {
                btn.textContent = 'Copied!';
                setTimeout(function () { btn.textContent = 'Copy URL'; }, 2000);
            });
        });
        metaLink.parentNode.appendChild(btn);
    }

    // Auto-dismiss success messages after 5 seconds
    var successMsg = document.querySelector('.aui-message-success');
    if (successMsg) {
        setTimeout(function () {
            successMsg.style.transition = 'opacity 0.5s';
            successMsg.style.opacity   = '0';
            setTimeout(function () { successMsg.remove(); }, 500);
        }, 5000);
    }
}());
