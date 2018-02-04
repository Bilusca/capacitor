import { Plugins } from './global';
import { mergeWebPlugins } from './web/index';

export * from './web/geolocation';
export * from './web/motion';

mergeWebPlugins(Plugins);
