<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "title">
        ${msg("eidas_select_your_country")}
    <#elseif section = "header">
        ${msg("eidas_select_your_country")}
    <#elseif section = "form">
        <style>
            .form-grid { 
                display: grid;
                grid-gap: 10px;
                grid-template-columns: repeat(auto-fill, minmax(90px, 1fr));
                justify-items: center;
                align-items: center;
                margin-bottom: 1rem;
                align-items: center;
            }
            .form-grid input:checked + label {
                border: 4px solid #0066cc;
                padding: 7px 13px;
            }
            .form-action {
                display: flex;
                margin-bottom: 0.5rem;
            }
            .form-action-input {
                margin: auto;
                padding: 0.75rem 2rem;
                border-radius: 4px;
            }
            .form-grid label {
                border: 1px solid #999; 
                padding: 10px 16px; 
                margin-bottom: 0;
            }
        </style>
        <div class="pf-l-stack__item select-auth-box-desc" style="margin-bottom: 1.5rem;">${msg("eidas_country_select_text")}</div>
        <form action="${url.loginAction}" class="${properties.kcFormClass!}" id="kc-citizen-form" method="post">
            <div class="form-grid">
	                <#list availablecountries as country>
                        <div>
                            <input name="country" value="${country}" type="radio" id="${country}" style="display: none;" required>
                            <label for="${country}">
                                <img width="50" src="${url.resourcesPath}/img/flags/${country}.png" alt="${country}" title="${country}">
                            </label>
                        </div>
	                </#list>
            </div>
            <div class="form-action">
                <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!} form-action-input"
                    type="submit" value="${msg("doSubmit")}"/>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>