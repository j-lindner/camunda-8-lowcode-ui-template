import React, { useEffect } from 'react';
import {useSelector} from 'react-redux';
import type { } from 'redux-thunk/extend-redux';
import adminFormService from '../service/AdminFormService';
import { newForm } from '@camunda-community/form-js-extended-jens';
import { IFormViewer } from '../store/model';

function FormPreview() {
  const previewData = useSelector((state: any) => state.adminForms.previewData)
  const schema = adminFormService.getSchema();
  const form = adminFormService.getCurrentForm();
  useEffect(() => {
    const container = document.querySelector('#task-form-preview');
    try {
      let variables = JSON.parse(previewData);
       if (container && schema) {
         container.innerHTML = '';

         let bpmnForm = null;
         //console.log(formViewer);

         bpmnForm = newForm({ container: container });

         bpmnForm.importSchema(schema, variables).then(
           function (data: any) {
               console.log(data);
           });
      }
    } catch (errors: any) {
      console.log(errors);
      container!.innerHTML = errors;
    }
  });



  return ( <div id="task-form-preview"></div>)

}

export default FormPreview;
