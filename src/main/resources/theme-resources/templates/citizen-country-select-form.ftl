<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "title">
        Citizen country selection
    <#elseif section = "header">
        Citizen country selection
    <#elseif section = "form">
        <p>Please select a citizen country:</p>
        <form action="${url.loginAction}" class="${properties.kcFormClass!}" id="kc-u2f-login-form" method="post">

            <datalist id="countrylist">
                <option value="CA">
                <option value="CB">
            </datalist>

            <div>
                <label for="country">Citizen Country</label>
                <input id="country" name="country" list="countrylist"/>
            </div>

            <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                   type="submit" value="${msg("doSubmit")}"/>
        </form>
    </#if>
</@layout.registrationLayout>
