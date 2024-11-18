import React, { useState, useEffect, Component, FC } from 'react';
import FormJsViewer from './FormJsViewer';
import FormIoViewer from './FormIoViewer';
import formService from '../service/FormService'
import { IFormViewer } from '../store/model';

const getFormFinder = (formViewer: IFormViewer): FC<IFormViewer> => {
  if (formService.customFormExists(formViewer.formKey)) {
    return formService.getCustomForm(formViewer.formKey)!;
  }
  if (formViewer.schema?.generator == 'formIo') {
    return FormIoViewer;
  }
  return FormJsViewer;
}

function FormResolver(formViewer: IFormViewer) {
  const FormFinder: FC<IFormViewer> = getFormFinder(formViewer);
  return (<FormFinder id={formViewer.id} formKey={formViewer.formKey} schema={formViewer.schema} variables={formViewer.variables} disabled={formViewer.disabled}></FormFinder>)
}

export default FormResolver;
