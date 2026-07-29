import { createApiClient } from './createApiClient';

export const userService = createApiClient('/api/v1/users');

export default userService;
