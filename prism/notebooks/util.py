import argparse
import datetime
import glob
import os

import yaml
from matplotlib import pyplot as plt


def collect_csv_files(includes, excludes, prefix, verbose):
    exclude_files = []
    for s in excludes:
        exclude_pattern = os.path.join(prefix, s)
        files = glob.glob(exclude_pattern)
        if not files:
            print(f"no csv_file found given the exclude parameter {s}")
        exclude_files.extend(files)
    if verbose:
        print(f"excluded files: {exclude_files}")
    include_files = []
    for s in includes:
        include_pattern = os.path.join(prefix, s)
        new_csv_files = glob.glob(include_pattern)
        if not new_csv_files:
            print(f"no csv_file found given the include parameter {s}")
        include_files.extend([f for f in new_csv_files if f not in exclude_files])
    include_files = list(set(include_files))  # remove duplicates
    if verbose:
        print(f"include files: {include_files}")
    return include_files


def make_dprint(x, verbose):
    def dprint(message):
        if verbose:
            print(datetime.datetime.now().strftime(f"%Y-%m-%d_%H-%M-%S: [{x}] {message}"))
    return dprint


def export_legend(ax, prefix, output_type, save, show):
    fig2 = plt.figure()
    ax2 = fig2.add_subplot()
    ax2.axis('off')
    legend = ax2.legend(*ax.get_legend_handles_labels(), frameon=False, loc='lower center', ncol=6, fontsize='x-small')
    fig = legend.figure
    fig.canvas.draw()
    bbox = legend.get_window_extent().transformed(fig.dpi_scale_trans.inverted())
    if save:
        fig.savefig(f"{prefix}_legend.{output_type}", dpi="figure", bbox_inches=bbox)
    if show:
        fig.show()


def dir_path(path):
    if os.path.isdir(path):
        return path
    else:
        raise argparse.ArgumentTypeError(f"{path} is not a valid path")


def conf_interval(s):
    if s.lower() == "sd":
        return "sd"
    try:
        v = int(s)
        if 0 < v < 100:
            return v
        else:
            raise argparse.ArgumentTypeError(f"invalid confidence interval: {s}")
    except ValueError:
        return None


def get_related_configuration(a_csv_file):
    yaml_file = os.path.splitext(a_csv_file)[0] + ".yaml"
    with open(yaml_file, "r") as stream:
        try:
            config = yaml.safe_load(stream)
        except yaml.YAMLError as exc:
            print(exc)
    return config


def get_plotting_arguments_parser(
    confidence_interval=None,
    remove_legend=False,
    external_legend=False,
    output_type="png",
    show=False,
    save=False,
    verbose=False,
    includes=("*.csv",),
    excludes=()
):
    parser = argparse.ArgumentParser(description="plotting arguments.")
    parser.add_argument("results_path", type=dir_path)
    parser.add_argument("-ci", "--confidence_interval", type=conf_interval, default=confidence_interval)
    parser.add_argument("--remove_legend", default=remove_legend, action="store_true")
    parser.add_argument("--external_legend", default=external_legend, action="store_true")
    parser.add_argument("--show", default=show, action="store_true")
    parser.add_argument("--save", default=save, action="store_true")
    parser.add_argument("--no_save", dest='save', action="store_false")
    parser.add_argument("--output_type", default=output_type, type=str)
    parser.add_argument("-o", "--out_prefix", default=None)
    parser.add_argument("-v", "--verbose", default=verbose, action="store_true")
    parser.add_argument("--includes", nargs="+", default=includes)
    parser.add_argument("--excludes", nargs="+", default=excludes)
    return parser
