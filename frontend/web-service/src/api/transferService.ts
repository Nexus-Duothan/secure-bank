import { createApiClient } from './createApiClient';

export const transferService = createApiClient('/api/v1/transfers');

export default transferService;
