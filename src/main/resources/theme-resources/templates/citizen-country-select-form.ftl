<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "title">
        Select your country
    <#elseif section = "header">
        Select your country
    <#elseif section = "form">
        <div class="pf-l-stack__item select-auth-box-desc">In order to continue, please select your nationality</div>
        <form action="${url.loginAction}" class="${properties.kcFormClass!}" id="kc-citizen-form" method="post">
            <div style="display: grid; grid-gap: 10px; grid-template-columns: repeat(auto-fill, minmax(90px, 1fr));">
	                <#list availablecountries as country>
                        <div>
                            <input name="country" value="${country}" type="radio" id="${country}" required>
                            <label for="${country}">
                                <img width="50" src="${url.resourcesPath}/img/flags/${country}.png" alt="${country}" title="${country}">
                            </label>
                        </div>
	                </#list>
            </div>

            <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                   type="submit" value="${msg("doSubmit")}"/>
        </form>
    </#if>
</@layout.registrationLayout>
