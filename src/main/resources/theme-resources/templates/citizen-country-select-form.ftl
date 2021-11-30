<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "title">
        Citizen country
    <#elseif section = "header">
        Citizen country
    <#elseif section = "form">
        <form action="${url.loginAction}" class="${properties.kcFormClass!}" id="kc-citizen-form" method="post">

            <div>
                <label for="country">Citizen Country</label>
                <select id="country" name="country">
                    <option hidden disabled selected value></option>
	                <#list availablecountries as country>
	                    <option value="${country}">${country}</option>
	                </#list>
                </select>
            </div>

            <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                   type="submit" value="${msg("doSubmit")}"/>
        </form>
    </#if>
</@layout.registrationLayout>
